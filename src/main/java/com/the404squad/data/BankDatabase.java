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
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    private final CategoryRepository categoryRepo = new CategoryRepository(CATEGORIES_JSON);
    private final List<Transaction> transactions = new ArrayList<>();
    private final Random rnd = new Random(404);

    private Account profile;
    private double balance;
    private int seq = 1;

    public BankDatabase() {
        try {
            categoryRepo.load();
            if (Files.exists(BANK_JSON)) {
                loadBank();
            } else {
                seedBank();
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger les donnees JSON : " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    //  Accesseurs
    // ------------------------------------------------------------------
    public Account account() { return profile; }
    public List<Transaction> transactions() { return transactions; }
    public double balance() { return balance; }
    public CategoryRepository categories() { return categoryRepo; }

    // ------------------------------------------------------------------
    //  Mutations (persistees)
    // ------------------------------------------------------------------

    /** Virement du compte courant vers une categorie : reduit le solde, ajoute une operation. */
    public synchronized double transfer(String categoryKey, double amount, LocalDate date) throws IOException {
        Category cat = Category.of(categoryKey);
        if (cat == null) throw new IllegalArgumentException("Categorie inconnue : " + categoryKey);
        if (amount <= 0) throw new IllegalArgumentException("Montant invalide");
        if (amount > balance) throw new IllegalArgumentException("Solde insuffisant");

        balance = round(balance - amount);
        String label = "Virement vers " + cat.label;
        transactions.add(new Transaction(seq++, date, label, round(amount), cat));
        saveBank();
        return balance;
    }

    /** Cree une categorie (delegue au depot) ; le catalogue JSON est persiste. */
    public synchronized Category addCategory(String name, String icon, String color, double budget) throws IOException {
        return categoryRepo.add(name, icon, color, budget);
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
          .append("},\n  \"transactions\": [\n");
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
    //  Graine de premier lancement (genere puis ecrit bank.json)
    // ------------------------------------------------------------------
    private void seedBank() throws IOException {
        this.profile = new Account("Lina Moreau", 20, "Bruxelles (Ixelles)", "BE71 0961 2345 6769", 3000.00);
        this.balance = 3000.00;
        generate();
        transactions.sort((a, b) -> a.date().compareTo(b.date()));
        saveBank();
    }

    private void add(LocalDate date, String label, double amount, Category cat) {
        transactions.add(new Transaction(seq++, date, label, round(amount), cat));
    }

    private void generate() {
        YearMonth start = YearMonth.of(2025, 1);
        YearMonth end = YearMonth.of(2026, 5);
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            generateMonth(ym);
        }
    }

    private void generateMonth(YearMonth ym) {
        // -------- REVENUS --------
        add(day(ym, 2), "Virement parents - soutien mensuel", 400, Category.of("REVENU"));
        double salaire = switch (ym.getMonthValue()) {
            case 1, 6, 7, 8 -> 620 + rnd.nextInt(120);
            case 5, 12 -> 280 + rnd.nextInt(80);
            default -> 430 + rnd.nextInt(140);
        };
        add(day(ym, 27), "Salaire job etudiant - Delhaize Flagey", salaire, Category.of("REVENU"));
        if (ym.getMonthValue() == 1 || ym.getMonthValue() == 4 || ym.getMonthValue() == 10) {
            add(day(ym, 15), "Bourse d'etudes - Federation Wallonie-Bruxelles", 750, Category.of("REVENU"));
        }

        // -------- LOYER --------
        add(day(ym, 3), "Loyer kot - Rue de la Paix, Ixelles", 575, Category.of("LOYER"));
        add(day(ym, 3), "Charges (eau / elec / internet)", 95, Category.of("LOYER"));

        // -------- EPARGNE & INVESTISSEMENT --------
        add(day(ym, 5), "Virement vers compte epargne", 100, Category.of("EPARGNE"));
        add(day(ym, 5), "Achat ETF MSCI World - Trade Republic", 50, Category.of("INVESTISSEMENT"));

        // -------- CREDIT --------
        add(day(ym, 8), "Remboursement credit - PC portable (Krefel)", 75, Category.of("CREDIT"));

        // -------- ABONNEMENTS --------
        add(day(ym, 1), "Basic-Fit abonnement salle", 24.99, Category.of("ABONNEMENTS"));
        add(day(ym, 6), "Spotify Premium Etudiant", 6.49, Category.of("ABONNEMENTS"));
        add(day(ym, 10), "Netflix (partage famille)", 5.99, Category.of("ABONNEMENTS"));
        add(day(ym, 15), "Proximus - forfait mobile", 15.00, Category.of("ABONNEMENTS"));

        // -------- ALIMENTATION --------
        String[] courses = {"Colruyt Ixelles", "Delhaize Flagey", "Lidl Matonge",
                "Carrefour Express", "Aldi Saint-Gilles", "Proxy Delhaize"};
        int nbCourses = 6 + rnd.nextInt(3);
        for (int i = 0; i < nbCourses; i++) {
            int d = 2 + rnd.nextInt(26);
            add(day(ym, d), courses[rnd.nextInt(courses.length)], 18 + rnd.nextDouble() * 32, Category.of("ALIMENTATION"));
        }
        String[] snacks = {"Maison Antoine - frites", "Exki", "Panos sandwich", "Starbucks ULB"};
        int nbSnacks = 3 + rnd.nextInt(3);
        for (int i = 0; i < nbSnacks; i++) {
            int d = 2 + rnd.nextInt(26);
            add(day(ym, d), snacks[rnd.nextInt(snacks.length)], 4 + rnd.nextDouble() * 8, Category.of("ALIMENTATION"));
        }

        // -------- TRANSPORT --------
        add(day(ym, 4), "STIB - abonnement MOBIB etudiant", 12.00, Category.of("TRANSPORT"));
        if (rnd.nextInt(2) == 0) {
            add(day(ym, 9 + rnd.nextInt(15)), "SNCB - Bruxelles <-> Namur (week-end)", 11.40, Category.of("TRANSPORT"));
        }
        if (rnd.nextInt(3) == 0) {
            add(day(ym, 9 + rnd.nextInt(15)), "Villo! location velo", 3.50, Category.of("TRANSPORT"));
        }

        // -------- LOISIRS --------
        String[] loisirs = {"Cafe Belga - Flagey", "UGC De Brouckere - cinema", "Delirium Cafe",
                "Concert Botanique", "Steam - jeu video", "Bar Le Coq",
                "Resto - Le Pain Quotidien", "Decathlon"};
        int nbLoisirs = 4 + rnd.nextInt(4);
        for (int i = 0; i < nbLoisirs; i++) {
            int d = 2 + rnd.nextInt(26);
            add(day(ym, d), loisirs[rnd.nextInt(loisirs.length)], 9 + rnd.nextDouble() * 36, Category.of("LOISIRS"));
        }

        // -------- SANTE --------
        if (rnd.nextInt(2) == 0) {
            add(day(ym, 12 + rnd.nextInt(10)), "Pharmacie Multipharma", 8 + rnd.nextDouble() * 22, Category.of("SANTE"));
        }

        // -------- Imprevus --------
        if (ym.getMonthValue() == 9) add(day(ym, 14), "Syllabus & materiel de cours", 145, Category.of("LOISIRS"));
        if (ym.getMonthValue() == 12) add(day(ym, 18), "Cadeaux de Noel", 90, Category.of("LOISIRS"));
        if (ym.getMonthValue() == 7) add(day(ym, 20), "Festival Couleur Cafe - ticket", 79, Category.of("LOISIRS"));
    }

    private static LocalDate day(YearMonth ym, int d) {
        return ym.atDay(Math.min(d, ym.lengthOfMonth()));
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
