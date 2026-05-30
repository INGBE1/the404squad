package com.the404squad.data;

import com.the404squad.model.Account;
import com.the404squad.model.Category;
import com.the404squad.model.Transaction;
import com.the404squad.util.Json;
import com.the404squad.util.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Store JSON : <strong>{@code data/bank.json} est la source de verite</strong>
 * (profil + solde + transactions). {@code data/categories.json} (via
 * {@link CategoryRepository}) porte le catalogue.
 *
 * Au premier lancement, si {@code bank.json} est absent, un releve realiste est
 * genere (etudiante a Bruxelles, jan 2025 -> mai 2026) puis ecrit en JSON. Ensuite,
 * c'est toujours le JSON qui fait foi, et chaque mutation (virement, nouvelle
 * categorie) est re-persistee sur disque.
 */
public final class BankDatabase {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path BANK_JSON = DATA_DIR.resolve("bank.json");
    private static final Path CATEGORIES_JSON = DATA_DIR.resolve("categories.json");
    private static final Path MERCHANTS_JSON = DATA_DIR.resolve("merchants.json");

    private final CategoryRepository categoryRepo = new CategoryRepository(CATEGORIES_JSON);
    private final MerchantRepository merchantRepo = new MerchantRepository(MERCHANTS_JSON);
    private final List<Transaction> transactions = new ArrayList<>();

    /**
     * "Enveloppes" : solde disponible mis de cote par categorie. Modele de
     * budget par enveloppe -> repartir de l'argent dans une categorie n'est PAS
     * une depense, c'est deplacer de l'argent du compte courant vers l'enveloppe.
     * Un achat se sert d'abord dans l'enveloppe ; le depassement retombe sur le
     * compte courant.
     */
    private final Map<String, Double> envelopes = new LinkedHashMap<>();

    private Account profile;
    private double balance;
    private int seq = 1;

    public BankDatabase() {
        try {
            categoryRepo.load();
            merchantRepo.load();
            if (Files.exists(BANK_JSON)) {
                loadBank();
            } else {
                seedBank();
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger les donnees JSON : " + e.getMessage(), e);
        }
    }

    /** Resultat d'une repartition : nouveau solde du compte + nouveau disponible de l'enveloppe. */
    public record AllocationResult(double balance, double available) {}

    /**
     * Resultat d'un paiement simule : nouveau solde, categorie devinee, nouveau
     * disponible de l'enveloppe, et la repartition du debit (part enveloppe / part compte).
     */
    public record PurchaseResult(double balance, String categoryKey, String categoryLabel,
                                 double available, double fromEnvelope, double fromMain) {}

    // ------------------------------------------------------------------
    //  Accesseurs
    // ------------------------------------------------------------------
    public Account account() { return profile; }
    public List<Transaction> transactions() { return transactions; }
    public double balance() { return balance; }
    public CategoryRepository categories() { return categoryRepo; }
    public MerchantRepository merchants() { return merchantRepo; }

    /** Solde disponible dans l'enveloppe d'une categorie (0 par defaut). */
    public double available(String categoryKey) {
        return round(envelopes.getOrDefault(categoryKey, 0.0));
    }

    // ------------------------------------------------------------------
    //  Mutations (persistees)
    // ------------------------------------------------------------------

    /**
     * Repartit de l'argent du compte courant vers l'enveloppe d'une categorie.
     * Ce n'est PAS une depense : l'argent quitte le compte courant pour
     * alimenter l'enveloppe (la limite de depense de la categorie).
     */
    public synchronized AllocationResult allocate(String categoryKey, double amount) throws IOException {
        Category cat = Category.of(categoryKey);
        if (cat == null) throw new IllegalArgumentException("Categorie inconnue : " + categoryKey);
        if (cat.kind == Category.Kind.REVENU) throw new IllegalArgumentException("Categorie non allouable");
        if (amount <= 0) throw new IllegalArgumentException("Montant invalide");
        if (amount > balance) throw new IllegalArgumentException("Solde insuffisant");

        balance = round(balance - amount);
        double avail = round(available(categoryKey) + amount);
        envelopes.put(categoryKey, avail);
        saveBank();
        return new AllocationResult(balance, avail);
    }

    /** Cree une categorie (delegue au depot) ; le catalogue JSON est persiste. */
    public synchronized Category addCategory(String name, String icon, String color, double budget) throws IOException {
        return categoryRepo.add(name, icon, color, budget);
    }

    /**
     * Simule un paiement chez un marchand : l'app devine la categorie a partir
     * du libelle (ex. "Uber Eats" -> Alimentation) ; si elle ne sait pas, le
     * paiement tombe dans AUTRES.
     *
     * Le debit se sert d'abord dans l'enveloppe de la categorie ; si le montant
     * depasse le disponible de l'enveloppe, le reste est preleve sur le compte
     * courant. Echoue si enveloppe + compte ne couvrent pas la depense.
     */
    public synchronized PurchaseResult purchase(String merchant, double amount, LocalDate date) throws IOException {
        if (merchant == null || merchant.isBlank()) throw new IllegalArgumentException("Marchand requis");
        if (amount <= 0) throw new IllegalArgumentException("Montant invalide");

        String key = merchantRepo.resolve(merchant);
        Category cat = Category.of(key);
        if (cat == null) cat = Category.of(MerchantRepository.FALLBACK);
        if (cat == null) throw new IllegalArgumentException("Categorie AUTRES manquante dans le catalogue");
        key = cat.name();

        double envAvail = available(key);
        if (amount > round(envAvail + balance)) throw new IllegalArgumentException("Solde insuffisant");

        double fromEnvelope = Math.min(envAvail, amount);
        double fromMain = round(amount - fromEnvelope);
        envelopes.put(key, round(envAvail - fromEnvelope));
        balance = round(balance - fromMain);

        transactions.add(new Transaction(seq++, date, merchant.trim(), round(amount), cat));
        saveBank();
        return new PurchaseResult(balance, key, cat.label, available(key),
                round(fromEnvelope), fromMain);
    }

    // ------------------------------------------------------------------
    //  Chargement / sauvegarde de bank.json
    // ------------------------------------------------------------------
    private void loadBank() throws IOException {
        Map<String, Object> root = JsonParser.asObject(JsonParser.parse(Files.readString(BANK_JSON)));
        Map<String, Object> acc = JsonParser.asObject(root.get("account"));
        this.balance = JsonParser.asDouble(acc.get("balance"));
        this.profile = new Account(
                JsonParser.asString(acc.get("holder")),
                JsonParser.asInt(acc.get("age")),
                JsonParser.asString(acc.get("city")),
                JsonParser.asString(acc.get("iban")),
                balance);

        // Enveloppes (disponible par categorie) ; on ignore les categories disparues.
        envelopes.clear();
        Object env = root.get("envelopes");
        if (env != null) {
            for (Map.Entry<String, Object> e : JsonParser.asObject(env).entrySet()) {
                if (Category.exists(e.getKey())) {
                    envelopes.put(e.getKey(), round(JsonParser.asDouble(e.getValue())));
                }
            }
        }

        int maxId = 0;
        for (Object o : JsonParser.asArray(root.get("transactions"))) {
            Map<String, Object> m = JsonParser.asObject(o);
            int id = JsonParser.asInt(m.get("id"));
            Category cat = Category.of(JsonParser.asString(m.get("category")));
            if (cat == null) continue; // categorie supprimee : on ignore l'operation
            transactions.add(new Transaction(
                    id,
                    LocalDate.parse(JsonParser.asString(m.get("date"))),
                    JsonParser.asString(m.get("label")),
                    JsonParser.asDouble(m.get("amount")),
                    cat));
            maxId = Math.max(maxId, id);
        }
        transactions.sort((a, b) -> a.date().compareTo(b.date()));
        this.seq = maxId + 1;
    }

    private void saveBank() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"account\": {")
          .append("\"holder\":").append(Json.str(profile.holder())).append(", ")
          .append("\"age\":").append(profile.age()).append(", ")
          .append("\"city\":").append(Json.str(profile.city())).append(", ")
          .append("\"iban\":").append(Json.str(profile.iban())).append(", ")
          .append("\"balance\":").append(Json.num(balance))
          .append("},\n  \"envelopes\": {");
        boolean firstEnv = true;
        for (Map.Entry<String, Double> e : envelopes.entrySet()) {
            if (!firstEnv) sb.append(",");
            firstEnv = false;
            sb.append("\"").append(e.getKey()).append("\":").append(Json.num(e.getValue()));
        }
        sb.append("},\n  \"transactions\": [\n");
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            sb.append("    {")
              .append("\"id\":").append(t.id()).append(", ")
              .append("\"date\":").append(Json.str(t.date().toString())).append(", ")
              .append("\"label\":").append(Json.str(t.label())).append(", ")
              .append("\"amount\":").append(Json.num(t.amount())).append(", ")
              .append("\"category\":").append(Json.str(t.category().name()))
              .append("}").append(i == transactions.size() - 1 ? "\n" : ",\n");
        }
        sb.append("  ]\n}\n");
        Files.createDirectories(DATA_DIR.toAbsolutePath());
        Files.writeString(BANK_JSON, sb.toString());
    }

    // ------------------------------------------------------------------
    //  Graine de premier lancement (ecrit bank.json)
    // ------------------------------------------------------------------
    private void seedBank() throws IOException {
        // Releve realiste d'une etudiante a Bruxelles : 6 mois d'historique avec
        // des operations reparties dans les differentes categories (loyer, courses,
        // livraison, transport, abonnements, loisirs, sante, + un marchand inconnu).
        LocalDate today = LocalDate.now();
        YearMonth current = YearMonth.from(today);
        for (int back = 5; back >= 0; back--) {
            seedMonth(current.minusMonths(back), today, back == 0);
        }
        transactions.sort((a, b) -> a.date().compareTo(b.date()));

        // Quelques enveloppes deja alimentees pour le mois en cours : l'utilisatrice
        // a mis de l'argent de cote pour limiter ses depenses dans ces categories.
        envelopes.put("ALIMENTATION", 120.00);
        envelopes.put("LOISIRS", 50.00);
        envelopes.put("TRANSPORT", 30.00);

        // Solde du compte = revenus - depenses - argent place en enveloppes.
        // (Conservation : solde + somme des enveloppes = revenus - depenses.)
        double income = 0, expense = 0;
        for (Transaction t : transactions) {
            if (t.category().kind == Category.Kind.REVENU) income += t.amount();
            else expense += t.amount();
        }
        double inEnvelopes = 0;
        for (double d : envelopes.values()) inEnvelopes += d;
        this.balance = round(income - expense - inEnvelopes);

        this.profile = new Account("Lina Moreau", 20, "Bruxelles (Ixelles)",
                "BE71 0961 2345 6769", balance);
        saveBank();
    }

    /** Genere le panier d'operations d'un mois (salaire + depenses variees). */
    private void seedMonth(YearMonth ym, LocalDate today, boolean isCurrent) {
        int s = ym.getMonthValue();   // graine de variation deterministe (mois)
        add(ym, 1,  "Salaire",              2000.00,        "REVENU",       today, isCurrent);
        add(ym, 3,  "Loyer studio Ixelles",  650.00,        "LOYER",        today, isCurrent);
        add(ym, 4,  "Netflix",                13.49,        "ABONNEMENTS",  today, isCurrent);
        add(ym, 5,  "Spotify Premium",        10.99,        "ABONNEMENTS",  today, isCurrent);
        add(ym, 6,  "STIB abonnement",        49.00,        "TRANSPORT",    today, isCurrent);
        add(ym, 8,  "Delhaize courses",       42.00 + s % 7, "ALIMENTATION", today, isCurrent);
        add(ym, 10, "Uber Eats",              18.50 + s % 5, "ALIMENTATION", today, isCurrent);
        add(ym, 12, "Carrefour Express",      27.00 + s % 9, "ALIMENTATION", today, isCurrent);
        add(ym, 14, "Kinepolis",              12.50,        "LOISIRS",      today, isCurrent);
        add(ym, 16, "Shell essence",          38.00 + s % 8, "TRANSPORT",    today, isCurrent);
        add(ym, 18, "Deliveroo",              16.90 + s % 6, "ALIMENTATION", today, isCurrent);
        if (s % 2 == 0) add(ym, 20, "Multipharma",       13.20, "SANTE",   today, isCurrent);
        add(ym, 22, "Steam",                  19.99,        "LOISIRS",      today, isCurrent);
        add(ym, 24, "Colruyt courses",        35.00 + s % 6, "ALIMENTATION", today, isCurrent);
        if (s % 3 == 0) add(ym, 26, "Decathlon",         34.90, "LOISIRS", today, isCurrent);
        if (s % 2 == 1) add(ym, 27, "Pharmacie de garde", 8.60, "SANTE",   today, isCurrent);
        add(ym, 28, "Carrefour Express",      23.40 + s % 5, "ALIMENTATION", today, isCurrent);
        add(ym, 29, "Bol.com",                24.99,        "AUTRES",       today, isCurrent);
    }

    /** Ajoute une operation datee ; ignore toute date future pour le mois en cours. */
    private void add(YearMonth ym, int day, String label, double amount,
                     String catKey, LocalDate today, boolean isCurrent) {
        LocalDate d = ym.atDay(Math.min(day, ym.lengthOfMonth()));
        if (isCurrent && d.isAfter(today)) return;   // pas d'operations dans le futur
        Category cat = Category.of(catKey);
        if (cat == null) return;
        transactions.add(new Transaction(seq++, d, label, round(amount), cat));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ------------------------------------------------------------------
    //  Export SQL (livrable "base de donnees") - inchange dans l'esprit
    // ------------------------------------------------------------------
    public void exportSql(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Base de donnees generee par the404squad - gestionnaire de compte\n");
        sb.append("-- Titulaire : ").append(profile.holder())
          .append(", ").append(profile.age()).append(" ans, ").append(profile.city()).append("\n");
        sb.append("-- IBAN : ").append(profile.iban()).append("\n\n");

        sb.append("DROP TABLE IF EXISTS transaction;\n");
        sb.append("DROP TABLE IF EXISTS account;\n\n");

        sb.append("CREATE TABLE account (\n")
          .append("    id               INTEGER PRIMARY KEY,\n")
          .append("    holder           VARCHAR(100) NOT NULL,\n")
          .append("    age              INTEGER NOT NULL,\n")
          .append("    city             VARCHAR(100) NOT NULL,\n")
          .append("    iban             VARCHAR(40)  NOT NULL,\n")
          .append("    balance          DECIMAL(10,2) NOT NULL\n")
          .append(");\n\n");

        sb.append("CREATE TABLE transaction (\n")
          .append("    id        INTEGER PRIMARY KEY,\n")
          .append("    op_date   DATE NOT NULL,\n")
          .append("    label     VARCHAR(120) NOT NULL,\n")
          .append("    amount    DECIMAL(10,2) NOT NULL,\n")
          .append("    category  VARCHAR(40) NOT NULL,\n")
          .append("    kind      VARCHAR(20) NOT NULL\n")
          .append(");\n\n");

        sb.append(String.format(
                "INSERT INTO account VALUES (1, '%s', %d, '%s', '%s', %.2f);%n%n",
                profile.holder(), profile.age(), profile.city(), profile.iban(), balance));

        sb.append("INSERT INTO transaction (id, op_date, label, amount, category, kind) VALUES\n");
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            sb.append(String.format("  (%d, '%s', '%s', %.2f, '%s', '%s')%s%n",
                    t.id(), t.date(), t.label().replace("'", "''"),
                    t.amount(), t.category().name(), t.category().kind.name(),
                    i == transactions.size() - 1 ? ";" : ","));
        }

        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, sb.toString());
    }
}
