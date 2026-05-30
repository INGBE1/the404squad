package com.the404squad.service;

import com.the404squad.data.BankDatabase;
import com.the404squad.model.Account;
import com.the404squad.model.Category;
import com.the404squad.model.Transaction;
import com.the404squad.util.Json;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcule les statistiques (revenus, depenses, epargne, ...) a partir du
 * releve, et serialise les resultats en JSON pour le dashboard.
 */
public final class StatsService {

    private final BankDatabase db;

    public StatsService(BankDatabase db) {
        this.db = db;
    }

    // ------------------------------------------------------------------
    //  Agregats
    // ------------------------------------------------------------------

    private record Totals(double revenus, double depenses, double epargne,
                          double investissement, double credit) {
        double resteAVivre() { return revenus - depenses - epargne - investissement - credit; }
        double effortFinancier() { return epargne + investissement + credit; }
    }

    private Totals totals(List<Transaction> txs) {
        double rev = 0, dep = 0, ep = 0, inv = 0, cred = 0;
        for (Transaction t : txs) {
            switch (t.category().kind) {
                case REVENU         -> rev += t.amount();
                case DEPENSE        -> dep += t.amount();
                case EPARGNE        -> ep += t.amount();
                case INVESTISSEMENT -> inv += t.amount();
                case CREDIT         -> cred += t.amount();
            }
        }
        return new Totals(rev, dep, ep, inv, cred);
    }

    private List<Transaction> inMonth(YearMonth ym) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : db.transactions()) {
            if (YearMonth.from(t.date()).equals(ym)) out.add(t);
        }
        return out;
    }

    private List<Transaction> inYear(int year) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : db.transactions()) {
            if (t.date().getYear() == year) out.add(t);
        }
        return out;
    }

    /** Solde du compte courant : valeur stockee (source de verite JSON), maj a chaque virement. */
    private double soldeCourant(LocalDate asOf) {
        return db.balance();
    }

    /** Patrimoine cumule pour un kind donne (epargne / investissement) jusqu'a aujourd'hui. */
    private double cumul(Category.Kind kind, LocalDate asOf) {
        double total = 0;
        for (Transaction t : db.transactions()) {
            if (t.category().kind == kind && !t.date().isAfter(asOf)) total += t.amount();
        }
        return total;
    }

    // ------------------------------------------------------------------
    //  Endpoints JSON
    // ------------------------------------------------------------------

    /** Profil + soldes globaux affiches dans l'en-tete. */
    public String overviewJson(YearMonth month, int year, LocalDate today) {
        Account a = db.account();
        Totals m = totals(inMonth(month));
        Totals y = totals(inYear(year));

        return "{"
                + "\"holder\":" + Json.str(a.holder()) + ","
                + "\"age\":" + a.age() + ","
                + "\"city\":" + Json.str(a.city()) + ","
                + "\"iban\":" + Json.str(a.iban()) + ","
                + "\"soldeCourant\":" + Json.num(soldeCourant(today)) + ","
                + "\"epargneCumulee\":" + Json.num(cumul(Category.Kind.EPARGNE, today)) + ","
                + "\"investCumule\":" + Json.num(cumul(Category.Kind.INVESTISSEMENT, today)) + ","
                + "\"month\":" + totalsJson(m) + ","
                + "\"year\":" + totalsJson(y)
                + "}";
    }

    private String totalsJson(Totals t) {
        return "{"
                + "\"revenus\":" + Json.num(t.revenus()) + ","
                + "\"depenses\":" + Json.num(t.depenses()) + ","
                + "\"epargne\":" + Json.num(t.epargne()) + ","
                + "\"investissement\":" + Json.num(t.investissement()) + ","
                + "\"credit\":" + Json.num(t.credit()) + ","
                + "\"effortFinancier\":" + Json.num(t.effortFinancier()) + ","
                + "\"resteAVivre\":" + Json.num(t.resteAVivre())
                + "}";
    }

    /** Catalogue complet des categories (built-in + custom) pour le client mobile. */
    public String categoriesJson() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Category c : Category.values()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"key\":").append(Json.str(c.key)).append(",")
              .append("\"labelFr\":").append(Json.str(c.label)).append(",")
              .append("\"labelEn\":").append(Json.str(c.labelEn)).append(",")
              .append("\"color\":").append(Json.str(c.color)).append(",")
              .append("\"icon\":").append(Json.str(c.icon)).append(",")
              .append("\"kind\":").append(Json.str(c.kind.name())).append(",")
              .append("\"budget\":").append(Json.num(c.defaultBudget))
              .append("}");
        }
        return sb.append("]").toString();
    }

    /** Catalogue de marchands (nom + icone + categorie cible) pour la simulation de paiement. */
    public String merchantsJson() {
        return db.merchants().toJson();
    }

    /**
     * Mini-IA d'epargne : extrapole les depenses des 7 derniers jours pour estimer
     * combien l'utilisateur mettrait de cote dans le futur s'il garde le meme rythme.
     *
     * Methode (heuristique, explicable) :
     *   1. on additionne les depenses (kind DEPENSE) de la fenetre glissante des 7 derniers jours,
     *   2. on en deduit une depense quotidienne moyenne, puis une depense mensuelle (x30),
     *   3. on compare au revenu mensuel moyen observe (total REVENU / nb de mois d'historique),
     *   4. l'epargne mensuelle = revenu - depense projetee ; on l'accumule sur 12 mois.
     * Renvoie aussi un "level" (positive / tight / negative) pour le message cote client.
     */
    public String forecastJson(LocalDate today) {
        // 1) Depenses de la semaine glissante [today-6 ; today].
        LocalDate weekStart = today.minusDays(6);
        double weekSpend = 0;
        for (Transaction t : db.transactions()) {
            if (t.category().kind == Category.Kind.DEPENSE
                    && !t.date().isBefore(weekStart) && !t.date().isAfter(today)) {
                weekSpend += t.amount();
            }
        }
        // 2) Projection : moyenne journaliere -> depense mensuelle estimee.
        double dailyAvg = weekSpend / 7.0;
        double monthlySpend = dailyAvg * 30.0;

        // 3) Revenu mensuel moyen observe dans l'historique.
        double totalRevenu = 0;
        for (Transaction t : db.transactions()) {
            if (t.category().kind == Category.Kind.REVENU) totalRevenu += t.amount();
        }
        double monthlyIncome = totalRevenu / Math.max(1, monthsCovered());

        // 4) Epargne mensuelle estimee et niveau de sante budgetaire.
        double monthlySavings = monthlyIncome - monthlySpend;
        String level = monthlySavings <= 0 ? "negative"
                : (monthlyIncome > 0 && monthlySavings < 0.20 * monthlyIncome) ? "tight" : "positive";

        // Serie cumulee sur 12 mois (point de depart 0).
        StringBuilder series = new StringBuilder("[");
        for (int m = 0; m <= 12; m++) {
            if (m > 0) series.append(",");
            series.append("{\"month\":").append(m)
                  .append(",\"savings\":").append(Json.num(r2(monthlySavings * m)))
                  .append("}");
        }
        series.append("]");

        return "{"
                + "\"weekSpend\":" + Json.num(r2(weekSpend)) + ","
                + "\"dailyAvg\":" + Json.num(r2(dailyAvg)) + ","
                + "\"monthlyIncome\":" + Json.num(r2(monthlyIncome)) + ","
                + "\"monthlySpend\":" + Json.num(r2(monthlySpend)) + ","
                + "\"monthlySavings\":" + Json.num(r2(monthlySavings)) + ","
                + "\"year1\":" + Json.num(r2(monthlySavings * 12)) + ","
                + "\"level\":" + Json.str(level) + ","
                + "\"series\":" + series
                + "}";
    }

    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    /** Solde disponible de chaque enveloppe (categorie allouable) : montant restant a depenser. */
    public String envelopesJson() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Category c : Category.values()) {
            if (c.kind == Category.Kind.REVENU) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"key\":").append(Json.str(c.name())).append(",")
              .append("\"available\":").append(Json.num(db.available(c.name())))
              .append("}");
        }
        return sb.append("]").toString();
    }

    /** Repartition par categorie de DEPENSE pour un mois donne (camembert). */
    public String byCategoryJson(YearMonth month) {
        Map<Category, Double> map = new LinkedHashMap<>();
        for (Transaction t : inMonth(month)) {
            if (t.category().kind == Category.Kind.DEPENSE) {
                map.merge(t.category(), t.amount(), Double::sum);
            }
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Category c : Category.values()) {
            if (!map.containsKey(c)) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"category\":").append(Json.str(c.label)).append(",")
              .append("\"color\":").append(Json.str(c.color)).append(",")
              .append("\"total\":").append(Json.num(map.get(c)))
              .append("}");
        }
        return sb.append("]").toString();
    }

    /** Serie sur 12 mois : revenus, depenses, effort financier (graphique en barres). */
    public String monthlySeriesJson(int year) {
        StringBuilder sb = new StringBuilder("[");
        for (int mo = 1; mo <= 12; mo++) {
            YearMonth ym = YearMonth.of(year, mo);
            Totals t = totals(inMonth(ym));
            if (mo > 1) sb.append(",");
            sb.append("{")
              .append("\"month\":").append(mo).append(",")
              .append("\"label\":").append(Json.str(moisFr(mo))).append(",")
              .append("\"revenus\":").append(Json.num(t.revenus())).append(",")
              .append("\"depenses\":").append(Json.num(t.depenses())).append(",")
              .append("\"effort\":").append(Json.num(t.effortFinancier()))
              .append("}");
        }
        return sb.append("]").toString();
    }

    /** Liste des transactions d'un mois (tableau), de la plus recente a la plus ancienne. */
    public String transactionsJson(YearMonth month) {
        List<Transaction> txs = inMonth(month);
        txs.sort((a, b) -> b.date().compareTo(a.date()));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < txs.size(); i++) {
            Transaction t = txs.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"id\":").append(t.id()).append(",")
              .append("\"date\":").append(Json.str(t.date().toString())).append(",")
              .append("\"label\":").append(Json.str(t.label())).append(",")
              .append("\"amount\":").append(Json.num(t.signedAmount())).append(",")
              .append("\"category\":").append(Json.str(t.category().label)).append(",")
              .append("\"color\":").append(Json.str(t.category().color))
              .append("}");
        }
        return sb.append("]").toString();
    }

    /**
     * Budgets par categorie de DEPENSE pour un mois donne, avec depense reelle,
     * pourcentage de consommation et statut (ok / warn / over) pour les alertes.
     * Les budgets modifies par l'utilisateur sont passes en overrides (libelle enum -> montant).
     */
    public String budgetsJson(YearMonth month, Map<String, Double> overrides) {
        // Depense reelle par categorie sur le mois.
        Map<Category, Double> spent = new LinkedHashMap<>();
        for (Transaction t : inMonth(month)) {
            if (t.category().kind == Category.Kind.DEPENSE) {
                spent.merge(t.category(), t.amount(), Double::sum);
            }
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Category c : Category.values()) {
            if (c.kind != Category.Kind.DEPENSE) continue;
            double budget = overrides.getOrDefault(c.name(), c.defaultBudget);
            double dep = spent.getOrDefault(c, 0.0);
            double pct = budget > 0 ? (dep / budget) * 100.0 : (dep > 0 ? 999.0 : 0.0);
            String status = pct >= 100 ? "over" : (pct >= 80 ? "warn" : "ok");
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"category\":").append(Json.str(c.label)).append(",")
              .append("\"key\":").append(Json.str(c.name())).append(",")
              .append("\"color\":").append(Json.str(c.color)).append(",")
              .append("\"budget\":").append(Json.num(budget)).append(",")
              .append("\"spent\":").append(Json.num(dep)).append(",")
              .append("\"pct\":").append(Json.num(pct)).append(",")
              .append("\"status\":").append(Json.str(status))
              .append("}");
        }
        return sb.append("]").toString();
    }

    /**
     * Projection du patrimoine financier sur {@code years} annees, en deux scenarios :
     * epargne livret (~1 %/an) et avec investissement (~5 %/an). Interets composes mensuels,
     * en ajoutant chaque mois la contribution moyenne (epargne + invest) observee dans l'historique.
     * Renvoie un point par annee (0..years).
     */
    public String projectionJson(LocalDate today, int years) {
        // Point de depart = poche reellement mise de cote (epargne + investissement).
        // On projette ce capital, pas le compte courant (qui peut etre a decouvert).
        double allocated = cumul(Category.Kind.EPARGNE, today) + cumul(Category.Kind.INVESTISSEMENT, today);
        double start = allocated;

        // Contribution mensuelle moyenne = (epargne + invest) cumules / nb de mois d'historique.
        int months = Math.max(1, monthsCovered());
        double contribution = allocated / months;

        double rLivret = 0.01 / 12.0;   // ~1 %/an
        double rInvest = 0.05 / 12.0;   // ~5 %/an
        double livret = start, invest = start;

        StringBuilder sb = new StringBuilder("[");
        sb.append("{\"annee\":0,\"livret\":").append(Json.num(start))
          .append(",\"invest\":").append(Json.num(start)).append("}");
        for (int y = 1; y <= years; y++) {
            for (int m = 0; m < 12; m++) {
                livret = livret * (1 + rLivret) + contribution;
                invest = invest * (1 + rInvest) + contribution;
            }
            sb.append(",{\"annee\":").append(y)
              .append(",\"livret\":").append(Json.num(livret))
              .append(",\"invest\":").append(Json.num(invest))
              .append("}");
        }
        return sb.append("]").toString();
    }

    /** Nombre de mois couverts par l'historique des transactions (au moins 1). */
    private int monthsCovered() {
        YearMonth min = null, max = null;
        for (Transaction t : db.transactions()) {
            YearMonth ym = YearMonth.from(t.date());
            if (min == null || ym.isBefore(min)) min = ym;
            if (max == null || ym.isAfter(max)) max = ym;
        }
        if (min == null) return 1;
        return (int) (java.time.temporal.ChronoUnit.MONTHS.between(min, max) + 1);
    }

    /** Liste des mois disponibles dans le releve (pour le selecteur). */
    public String availableMonthsJson() {
        List<String> months = new ArrayList<>();
        for (Transaction t : db.transactions()) {
            String key = YearMonth.from(t.date()).toString();
            if (!months.contains(key)) months.add(key);
        }
        months.sort(java.util.Comparator.reverseOrder());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < months.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(Json.str(months.get(i)));
        }
        return sb.append("]").toString();
    }

    private static String moisFr(int m) {
        String[] noms = {"Jan", "Fev", "Mar", "Avr", "Mai", "Juin",
                "Juil", "Aout", "Sep", "Oct", "Nov", "Dec"};
        return noms[m - 1];
    }
}
