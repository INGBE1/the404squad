package com.the404squad.data;

import com.the404squad.model.Category;
import com.the404squad.util.Json;
import com.the404squad.util.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Charge / sauvegarde le catalogue de categories depuis {@code data/categories.json}.
 * Ce fichier est la source de verite : aucune categorie n'est codee en dur ailleurs
 * (hors graine de premier lancement ci-dessous, ecrite une seule fois si le fichier
 * est absent).
 */
public final class CategoryRepository {

    private final Path path;

    public CategoryRepository(Path path) { this.path = path; }

    /** Remplit le registre {@link Category} depuis le JSON (ou graine si absent). */
    public void load() throws IOException {
        Category.clear();
        if (!Files.exists(path)) {
            seedDefaults();
            return;
        }
        String json = Files.readString(path);
        List<Object> arr = JsonParser.asArray(JsonParser.parse(json));
        for (Object o : arr) {
            Map<String, Object> m = JsonParser.asObject(o);
            Category.register(new Category(
                    JsonParser.asString(m.get("key")),
                    JsonParser.asString(m.get("labelFr")),
                    JsonParser.asString(m.get("labelEn")),
                    JsonParser.asString(m.get("color")),
                    JsonParser.asString(m.get("icon")),
                    Category.Kind.valueOf(JsonParser.asString(m.get("kind"))),
                    JsonParser.asDouble(m.get("budget"))
            ));
        }
    }

    /** Cree une nouvelle categorie de depense, l'enregistre et persiste le catalogue. */
    public Category add(String name, String icon, String color, double budget) throws IOException {
        String key = "CUSTOM_" + System.currentTimeMillis();
        Category c = new Category(key, name, name, color, icon, Category.Kind.DEPENSE, budget);
        Category.register(c);
        save();
        return c;
    }

    /** Reserialise tout le catalogue (built-in + custom) vers le JSON. */
    public void save() throws IOException {
        StringBuilder sb = new StringBuilder("[\n");
        boolean first = true;
        for (Category c : Category.values()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  ").append(toJson(c));
        }
        sb.append("\n]\n");
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, sb.toString());
    }

    public static String toJson(Category c) {
        return "{"
                + "\"key\":" + Json.str(c.key) + ", "
                + "\"labelFr\":" + Json.str(c.label) + ", "
                + "\"labelEn\":" + Json.str(c.labelEn) + ", "
                + "\"color\":" + Json.str(c.color) + ", "
                + "\"icon\":" + Json.str(c.icon) + ", "
                + "\"kind\":" + Json.str(c.kind.name()) + ", "
                + "\"budget\":" + Json.num(c.defaultBudget)
                + "}";
    }

    // ------------------------------------------------------------------
    //  Graine de premier lancement (uniquement si data/categories.json absent)
    // ------------------------------------------------------------------
    private void seedDefaults() throws IOException {
        Category.clear();
        Category.register(new Category("REVENU", "Revenu", "Income", "#3b82f6", "💰", Category.Kind.REVENU, 0));
        Category.register(new Category("LOYER", "Loyer", "Rent", "#6366f1", "🏠", Category.Kind.DEPENSE, 0));
        Category.register(new Category("ALIMENTATION", "Alimentation", "Food", "#f59e0b", "🍔", Category.Kind.DEPENSE, 0));
        Category.register(new Category("TRANSPORT", "Transport", "Transport", "#06b6d4", "🚌", Category.Kind.DEPENSE, 0));
        Category.register(new Category("ABONNEMENTS", "Abonnements", "Subscriptions", "#8b5cf6", "📺", Category.Kind.DEPENSE, 0));
        Category.register(new Category("LOISIRS", "Loisirs", "Leisure", "#ec4899", "🎮", Category.Kind.DEPENSE, 0));
        Category.register(new Category("SANTE", "Santé", "Health", "#ef4444", "➕", Category.Kind.DEPENSE, 0));
        Category.register(new Category("EPARGNE", "Épargne", "Savings", "#22c55e", "🐷", Category.Kind.EPARGNE, 0));
        Category.register(new Category("INVESTISSEMENT", "Investissement", "Investment", "#14b8a6", "📈", Category.Kind.INVESTISSEMENT, 0));
        Category.register(new Category("CREDIT", "Crédit", "Credit", "#f97316", "💳", Category.Kind.CREDIT, 0));
        Category.register(new Category("AUTRES", "Autres", "Others", "#94a3b8", "❓", Category.Kind.DEPENSE, 0));
        save();
    }
}
