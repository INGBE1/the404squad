package com.the404squad.data;

import com.the404squad.model.Account;
import com.the404squad.model.Category;
import com.the404squad.model.Transaction;
import com.the404squad.util.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * "Base de donnees" en memoire : genere un releve bancaire realiste pour
 * Lina Moreau, etudiante de 20 ans a Bruxelles (Ixelles).
 *
 * Les donnees couvrent janvier 2025 -> mai 2026 afin que les vues
 * "par mois" ET "par annee" soient bien remplies.
 *
 * La generation est deterministe (seed fixe) : le releve est donc le meme
 * a chaque lancement, et peut etre exporte en JSON via exportJson().
 */
public final class BankDatabase {

    private final Account account;
    private final List<Transaction> transactions = new ArrayList<>();
    private final Random rnd = new Random(404); // seed = clin d'oeil a l'equipe

    public BankDatabase() {
        this.account = new Account(
                "Lina Moreau", 20, "Bruxelles (Ixelles)",
                "BE71 0961 2345 6769", 850.00);
        generate();
    }

    public Account account() { return account; }

    public List<Transaction> transactions() { return transactions; }

    // ------------------------------------------------------------------
    //  Generation du releve
    // ------------------------------------------------------------------

    private int seq = 1;

    private void add(LocalDate date, String label, double amount, Category cat) {
        transactions.add(new Transaction(seq++, date, label, round(amount), cat));
    }

    private void generate() {
        YearMonth start = YearMonth.of(2025, 1);
        YearMonth end = YearMonth.of(2026, 5);

        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            generateMonth(ym);
        }
        // Tri chronologique puis renumerotation propre des ids.
        transactions.sort((a, b) -> a.date().compareTo(b.date()));
    }

    private void generateMonth(YearMonth ym) {
        int year = ym.getYear();
        // -------- REVENUS --------
        add(day(ym, 2), "Virement parents - soutien mensuel", 400, Category.REVENU);
        // Job etudiant : variable selon l'activite (examens = moins d'heures).
        double salaire = switch (ym.getMonthValue()) {
            case 1, 6, 7, 8 -> 620 + rnd.nextInt(120); // vacances / fin d'annee : plus d'heures
            case 5, 12 -> 280 + rnd.nextInt(80);        // blocus : moins d'heures
            default -> 430 + rnd.nextInt(140);
        };
        add(day(ym, 27), "Salaire job etudiant - Delhaize Flagey", salaire, Category.REVENU);
        // Bourse de la Communaute francaise : versee par trimestre.
        if (ym.getMonthValue() == 1 || ym.getMonthValue() == 4
                || ym.getMonthValue() == 10) {
            add(day(ym, 15), "Bourse d'etudes - Federation Wallonie-Bruxelles", 750, Category.REVENU);
        }

        // -------- LOYER --------
        add(day(ym, 3), "Loyer kot - Rue de la Paix, Ixelles", 575, Category.LOYER);
        add(day(ym, 3), "Charges (eau / elec / internet)", 95, Category.LOYER);

        // -------- EPARGNE & INVESTISSEMENT (ordre permanent) --------
        add(day(ym, 5), "Virement vers compte epargne", 100, Category.EPARGNE);
        add(day(ym, 5), "Achat ETF MSCI World - Trade Republic", 50, Category.INVESTISSEMENT);

        // -------- CREDIT (remboursement PC portable, 75 EUR/mois) --------
        add(day(ym, 8), "Remboursement credit - PC portable (Krefel)", 75, Category.CREDIT);

        // -------- ABONNEMENTS --------
        add(day(ym, 1), "Basic-Fit abonnement salle", 24.99, Category.ABONNEMENTS);
        add(day(ym, 6), "Spotify Premium Etudiant", 6.49, Category.ABONNEMENTS);
        add(day(ym, 10), "Netflix (partage famille)", 5.99, Category.ABONNEMENTS);
        add(day(ym, 15), "Proximus - forfait mobile", 15.00, Category.ABONNEMENTS);

        // -------- ALIMENTATION (courses + snacks etalees sur le mois) --------
        String[] courses = {
                "Colruyt Ixelles", "Delhaize Flagey", "Lidl Matonge",
                "Carrefour Express", "Aldi Saint-Gilles", "Proxy Delhaize"
        };
        int nbCourses = 6 + rnd.nextInt(3);
        for (int i = 0; i < nbCourses; i++) {
            int d = 2 + rnd.nextInt(26);
            double montant = 18 + rnd.nextDouble() * 32; // 18 - 50 EUR
            add(day(ym, d), courses[rnd.nextInt(courses.length)], montant, Category.ALIMENTATION);
        }
        // Quelques snacks / cafes etudiants
        String[] snacks = {"Maison Antoine - frites", "Exki", "Panos sandwich", "Starbucks ULB"};
        int nbSnacks = 3 + rnd.nextInt(3);
        for (int i = 0; i < nbSnacks; i++) {
            int d = 2 + rnd.nextInt(26);
            add(day(ym, d), snacks[rnd.nextInt(snacks.length)], 4 + rnd.nextDouble() * 8, Category.ALIMENTATION);
        }

        // -------- TRANSPORT --------
        add(day(ym, 4), "STIB - abonnement MOBIB etudiant", 12.00, Category.TRANSPORT);
        if (rnd.nextInt(2) == 0) {
            add(day(ym, 9 + rnd.nextInt(15)), "SNCB - Bruxelles <-> Namur (week-end)", 11.40, Category.TRANSPORT);
        }
        if (rnd.nextInt(3) == 0) {
            add(day(ym, 9 + rnd.nextInt(15)), "Villo! location velo", 3.50, Category.TRANSPORT);
        }

        // -------- LOISIRS --------
        String[] loisirs = {
                "Cafe Belga - Flagey", "UGC De Brouckere - cinema", "Delirium Cafe",
                "Concert Botanique", "Steam - jeu video", "Bar Le Coq",
                "Resto - Le Pain Quotidien", "Decathlon"
        };
        int nbLoisirs = 4 + rnd.nextInt(4);
        for (int i = 0; i < nbLoisirs; i++) {
            int d = 2 + rnd.nextInt(26);
            double montant = 9 + rnd.nextDouble() * 36; // 9 - 45 EUR
            add(day(ym, d), loisirs[rnd.nextInt(loisirs.length)], montant, Category.LOISIRS);
        }

        // -------- SANTE (ponctuel) --------
        if (rnd.nextInt(2) == 0) {
            add(day(ym, 12 + rnd.nextInt(10)), "Pharmacie Multipharma", 8 + rnd.nextDouble() * 22, Category.SANTE);
        }

        // -------- Imprevu occasionnel (rentree, cadeaux, voyage...) --------
        if (ym.getMonthValue() == 9) {
            add(day(ym, 14), "Syllabus & materiel de cours", 145, Category.LOISIRS);
        }
        if (ym.getMonthValue() == 12) {
            add(day(ym, 18), "Cadeaux de Noel", 90, Category.LOISIRS);
        }
        if (ym.getMonthValue() == 7) {
            add(day(ym, 20), "Festival Couleur Cafe - ticket", 79, Category.LOISIRS);
        }
        if (year == year) { /* no-op, garde la structure lisible */ }
    }

    /** Renvoie une date valide dans le mois (clamp au dernier jour si besoin). */
    private static LocalDate day(YearMonth ym, int d) {
        int clamped = Math.min(d, ym.lengthOfMonth());
        return ym.atDay(clamped);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ------------------------------------------------------------------
    //  Export JSON (livrable "base de donnees")
    // ------------------------------------------------------------------

    /**
     * Ecrit le compte + les transactions du releve dans un fichier .json
     * (objet avec "account" et un tableau "transactions"). Appele au
     * demarrage de l'app. JSON construit a la main via util/Json (zero dependance).
     */
    public void exportJson(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // -------- account --------
        sb.append("  \"account\": {\n")
          .append("    \"holder\": ").append(Json.str(account.holder())).append(",\n")
          .append("    \"age\": ").append(account.age()).append(",\n")
          .append("    \"city\": ").append(Json.str(account.city())).append(",\n")
          .append("    \"iban\": ").append(Json.str(account.iban())).append(",\n")
          .append("    \"starting_balance\": ").append(Json.num(account.startingBalance())).append(",\n")
          .append("    \"transactions_count\": ").append(transactions.size()).append("\n")
          .append("  },\n");

        // -------- transactions --------
        sb.append("  \"transactions\": [\n");
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            sb.append("    {")
              .append("\"id\": ").append(t.id()).append(", ")
              .append("\"op_date\": ").append(Json.str(t.date().toString())).append(", ")
              .append("\"label\": ").append(Json.str(t.label())).append(", ")
              .append("\"amount\": ").append(Json.num(t.amount())).append(", ")
              .append("\"category\": ").append(Json.str(t.category().name())).append(", ")
              .append("\"kind\": ").append(Json.str(t.category().kind.name()))
              .append("}")
              .append(i == transactions.size() - 1 ? "\n" : ",\n");
        }
        sb.append("  ]\n");

        sb.append("}\n");

        Files.createDirectories(path.getParent());
        Files.writeString(path, sb.toString());
    }
}
