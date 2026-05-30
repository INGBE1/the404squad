package com.the404squad.data;

import com.the404squad.util.Json;
import com.the404squad.util.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Catalogue de marchands depuis {@code data/merchants.json}. Chaque marchand
 * associe des mots-cles a une categorie : c'est ce qui permet a l'app de
 * "deviner" ou ranger un paiement (ex. "Uber Eats" -> ALIMENTATION).
 *
 * Si aucun marchand ne correspond, {@link #resolve(String)} renvoie {@code AUTRES}
 * : le paiement atterrit alors dans la categorie fourre-tout.
 */
public final class MerchantRepository {

    /** Fallback lorsqu'aucun marchand ne correspond. */
    public static final String FALLBACK = "AUTRES";

    public record Merchant(String name, String icon, String categoryKey, List<String> keywords) {}

    private final Path path;
    private final List<Merchant> merchants = new ArrayList<>();

    public MerchantRepository(Path path) { this.path = path; }

    public void load() throws IOException {
        merchants.clear();
        if (!Files.exists(path)) {
            seedDefaults();
            return;
        }
        for (Object o : JsonParser.asArray(JsonParser.parse(Files.readString(path)))) {
            Map<String, Object> m = JsonParser.asObject(o);
            List<String> kw = new ArrayList<>();
            Object k = m.get("keywords");
            if (k != null) {
                for (Object x : JsonParser.asArray(k)) kw.add(JsonParser.asString(x).toLowerCase());
            }
            merchants.add(new Merchant(
                    JsonParser.asString(m.get("name")),
                    JsonParser.asString(m.get("icon")),
                    JsonParser.asString(m.get("categoryKey")),
                    kw));
        }
    }

    public List<Merchant> all() { return merchants; }

    /**
     * Resout un libelle de marchand (saisi ou choisi) vers une cle de categorie.
     * Correspondance par nom exact, puis par mot-cle contenu dans la saisie.
     * Renvoie {@link #FALLBACK} (= AUTRES) si rien ne correspond.
     */
    public String resolve(String input) {
        if (input == null) return FALLBACK;
        String s = input.trim().toLowerCase();
        if (s.isEmpty()) return FALLBACK;
        for (Merchant m : merchants) {
            if (m.name().toLowerCase().equals(s)) return m.categoryKey();
        }
        for (Merchant m : merchants) {
            for (String kw : m.keywords()) {
                if (!kw.isBlank() && s.contains(kw)) return m.categoryKey();
            }
        }
        return FALLBACK;
    }

    /** Liste pour le client : nom + icone + categorie cible (sans les mots-cles). */
    public String toJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < merchants.size(); i++) {
            Merchant m = merchants.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"name\":").append(Json.str(m.name())).append(",")
              .append("\"icon\":").append(Json.str(m.icon())).append(",")
              .append("\"categoryKey\":").append(Json.str(m.categoryKey()))
              .append("}");
        }
        return sb.append("]").toString();
    }

    // ------------------------------------------------------------------
    //  Graine de premier lancement (uniquement si data/merchants.json absent)
    // ------------------------------------------------------------------
    private void seedDefaults() throws IOException {
        merchants.clear();
        merchants.add(new Merchant("Uber Eats", "🍔", "ALIMENTATION",
                List.of("uber eats", "ubereats", "mcdonald", "mcdo", "burger king", "kfc", "quick", "takeaway", "pizza", "resto", "restaurant")));
        merchants.add(new Merchant("Deliveroo", "🛵", "ALIMENTATION",
                List.of("deliveroo", "takeaway.com", "just eat")));
        merchants.add(new Merchant("Carrefour", "🛒", "ALIMENTATION",
                List.of("carrefour", "colruyt", "delhaize", "aldi", "lidl", "courses", "supermarche", "proxy")));
        merchants.add(new Merchant("Netflix", "📺", "ABONNEMENTS",
                List.of("netflix", "spotify", "disney", "amazon prime", "youtube premium", "abonnement")));
        merchants.add(new Merchant("STIB", "🚌", "TRANSPORT",
                List.of("stib", "mivb", "sncb", "de lijn", "tec", "bolt", "uber ride", "taxi", "train", "metro", "villo")));
        merchants.add(new Merchant("Shell", "⛽", "TRANSPORT",
                List.of("shell", "total", "esso", "q8", "texaco", "essence", "carburant", "station")));
        merchants.add(new Merchant("Pharmacie", "💊", "SANTE",
                List.of("pharmacie", "pharma", "multipharma", "medecin", "docteur", "hopital", "dentiste")));
        merchants.add(new Merchant("Steam", "🎮", "LOISIRS",
                List.of("steam", "playstation", "xbox", "nintendo", "ugc", "kinepolis", "cinema", "fnac", "decathlon", "concert", "jeu")));
        save();
    }

    private void save() throws IOException {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < merchants.size(); i++) {
            Merchant m = merchants.get(i);
            sb.append("  {")
              .append("\"name\":").append(Json.str(m.name())).append(", ")
              .append("\"icon\":").append(Json.str(m.icon())).append(", ")
              .append("\"categoryKey\":").append(Json.str(m.categoryKey())).append(", ")
              .append("\"keywords\":[");
            for (int j = 0; j < m.keywords().size(); j++) {
                if (j > 0) sb.append(",");
                sb.append(Json.str(m.keywords().get(j)));
            }
            sb.append("]}").append(i == merchants.size() - 1 ? "\n" : ",\n");
        }
        sb.append("]\n");
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, sb.toString());
    }
}
