package com.the404squad.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Categorie de mouvement bancaire.
 *
 * N'est plus un enum : les categories sont desormais des DONNEES chargees
 * depuis {@code data/categories.json} (source de verite). Un registre statique
 * conserve l'ordre d'insertion et permet de retrouver une categorie par sa cle.
 *
 * Chaque categorie porte un libelle FR + EN, une couleur, une icone (emoji),
 * un {@link Kind} (nature du mouvement) et un budget mensuel par defaut.
 */
public final class Category {

    /** Nature d'un mouvement : entree, depense, ou allocation (epargne / invest / credit). */
    public enum Kind { REVENU, DEPENSE, EPARGNE, INVESTISSEMENT, CREDIT }

    private static final Map<String, Category> REGISTRY = new LinkedHashMap<>();

    public final String key;
    public final String label;     // libelle FR (compat dashboard)
    public final String labelEn;   // libelle EN
    public final String color;
    public final String icon;
    public final Kind kind;
    public final double defaultBudget;

    public Category(String key, String label, String labelEn, String color,
                    String icon, Kind kind, double defaultBudget) {
        this.key = key;
        this.label = label;
        this.labelEn = labelEn;
        this.color = color;
        this.icon = icon;
        this.kind = kind;
        this.defaultBudget = defaultBudget;
    }

    /** Cle technique (ex. "LOYER"). Conserve le nom de l'ancien enum pour compat. */
    public String name() { return key; }

    // ---- Registre ----
    public static Collection<Category> values() { return REGISTRY.values(); }
    public static Category of(String key) { return REGISTRY.get(key); }
    public static boolean exists(String key) { return REGISTRY.containsKey(key); }
    public static void register(Category c) { REGISTRY.put(c.key, c); }
    public static void clear() { REGISTRY.clear(); }
}
