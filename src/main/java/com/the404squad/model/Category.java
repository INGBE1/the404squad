package com.the404squad.model;

/**
 * Categories de mouvements bancaires pour le gestionnaire de compte.
 * Chaque categorie porte un libelle FR, une couleur (pour les graphiques)
 * et un "kind" qui dit comment la traiter dans les statistiques.
 */
public enum Category {
    REVENU("Revenu", "#3b82f6", Kind.REVENU, 0),
    LOYER("Loyer", "#6366f1", Kind.DEPENSE, 520),
    ALIMENTATION("Alimentation", "#f59e0b", Kind.DEPENSE, 250),
    TRANSPORT("Transport", "#06b6d4", Kind.DEPENSE, 60),
    ABONNEMENTS("Abonnements", "#8b5cf6", Kind.DEPENSE, 45),
    LOISIRS("Loisirs", "#ec4899", Kind.DEPENSE, 120),
    SANTE("Sante", "#ef4444", Kind.DEPENSE, 40),
    EPARGNE("Epargne", "#22c55e", Kind.EPARGNE, 0),
    INVESTISSEMENT("Investissement", "#14b8a6", Kind.INVESTISSEMENT, 0),
    CREDIT("Credit", "#f97316", Kind.CREDIT, 0);

    /** Nature d'un mouvement : entree d'argent, depense de consommation, ou allocation (epargne/invest/credit). */
    public enum Kind { REVENU, DEPENSE, EPARGNE, INVESTISSEMENT, CREDIT }

    public final String label;
    public final String color;
    public final Kind kind;
    /** Budget mensuel par defaut (EUR) pour les categories de depense ; 0 pour les autres. */
    public final double defaultBudget;

    Category(String label, String color, Kind kind, double defaultBudget) {
        this.label = label;
        this.color = color;
        this.kind = kind;
        this.defaultBudget = defaultBudget;
    }
}
