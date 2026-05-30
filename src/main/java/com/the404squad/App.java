package com.the404squad;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.the404squad.data.BankDatabase;
import com.the404squad.service.StatsService;
import com.the404squad.util.Json;
import com.the404squad.util.JsonParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.HashMap;

/**
 * Gestionnaire de compte - the404squad.
 *
 * Application web en Java pur : serveur HTTP du JDK (com.sun.net.httpserver),
 * donnees en memoire, statistiques calculees cote serveur, dashboard
 * affiche cote client avec Chart.js.
 *
 * Lancement :  java -cp out com.the404squad.App   (voir run.cmd / run.sh)
 * Puis ouvrir :  http://localhost:8080
 */
public final class App {

    private static final int PORT = 8080;
    private static final Path WEB_DIR = Path.of("web");

    private final BankDatabase db = new BankDatabase();
    private final StatsService stats = new StatsService(db);

    public static void main(String[] args) throws IOException {
        new App().start();
    }

    private void start() throws IOException {
        // Export du "livrable base de donnees" au demarrage.
        Path sqlOut = Path.of("database", "bank.sql");
        db.exportSql(sqlOut);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.setExecutor(null);
        server.start();

        System.out.println("============================================================");
        System.out.println("  the404squad - Gestionnaire de compte");
        System.out.println("  Titulaire : " + db.account().holder()
                + " (" + db.account().age() + " ans, " + db.account().city() + ")");
        System.out.println("  Transactions generees : " + db.transactions().size());
        System.out.println("  Base SQL exportee      : " + sqlOut.toAbsolutePath());
        System.out.println("------------------------------------------------------------");
        System.out.println("  >> Ouvre ton navigateur sur : http://localhost:" + PORT);
        System.out.println("============================================================");
    }

    // ------------------------------------------------------------------
    //  API JSON
    // ------------------------------------------------------------------

    private void handleApi(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();

            // --- Mutations (POST) : persistées dans les fichiers JSON ---
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                handlePost(ex, path);
                return;
            }

            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());

            LocalDate today = LocalDate.now();
            YearMonth month = q.containsKey("month")
                    ? YearMonth.parse(q.get("month"))
                    : YearMonth.from(today);
            int year = q.containsKey("year")
                    ? Integer.parseInt(q.get("year"))
                    : month.getYear();

            int years = q.containsKey("years")
                    ? Integer.parseInt(q.get("years"))
                    : 10;

            String json = switch (path) {
                case "/api/overview"     -> stats.overviewJson(month, year, today);
                case "/api/by-category"  -> stats.byCategoryJson(month);
                case "/api/monthly"      -> stats.monthlySeriesJson(year);
                case "/api/transactions" -> stats.transactionsJson(month);
                case "/api/months"       -> stats.availableMonthsJson();
                case "/api/budgets"      -> stats.budgetsJson(month, budgetOverrides(q));
                case "/api/projection"   -> stats.projectionJson(today, years);
                case "/api/categories"   -> stats.categoriesJson();
                case "/api/merchants"    -> stats.merchantsJson();
                case "/api/envelopes"    -> stats.envelopesJson();
                case "/api/forecast"     -> stats.forecastJson(today);
                default -> null;
            };

            if (json == null) {
                send(ex, 404, "application/json", "{\"error\":\"not found\"}");
            } else {
                send(ex, 200, "application/json", json);
            }
        } catch (Exception e) {
            send(ex, 500, "application/json",
                    "{\"error\":" + com.the404squad.util.Json.str(String.valueOf(e.getMessage())) + "}");
        }
    }

    // ------------------------------------------------------------------
    //  Mutations JSON (POST)
    // ------------------------------------------------------------------
    private void handlePost(HttpExchange ex, String path) throws IOException {
        try {
            Map<String, Object> body = JsonParser.asObject(
                    JsonParser.parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));

            switch (path) {
                case "/api/allocate" -> {
                    String key = JsonParser.asString(body.get("categoryKey"));
                    double amount = JsonParser.asDouble(body.get("amount"));
                    var r = db.allocate(key, amount);
                    send(ex, 200, "application/json",
                            "{\"ok\":true,\"balance\":" + Json.num(r.balance())
                            + ",\"available\":" + Json.num(r.available()) + "}");
                }
                case "/api/category" -> {
                    String name = JsonParser.asString(body.get("name"));
                    String icon = JsonParser.asString(body.get("icon"));
                    String color = JsonParser.asString(body.get("color"));
                    double budget = JsonParser.asDouble(body.get("budget"));
                    if (name == null || name.isBlank()) {
                        send(ex, 400, "application/json", "{\"error\":\"name required\"}");
                        return;
                    }
                    var cat = db.addCategory(name.trim(), icon, color, budget);
                    send(ex, 200, "application/json", com.the404squad.data.CategoryRepository.toJson(cat));
                }
                case "/api/purchase" -> {
                    String merchant = JsonParser.asString(body.get("merchant"));
                    double amount = JsonParser.asDouble(body.get("amount"));
                    var r = db.purchase(merchant, amount, java.time.LocalDate.now());
                    send(ex, 200, "application/json",
                            "{\"ok\":true,\"balance\":" + Json.num(r.balance())
                            + ",\"categoryKey\":" + Json.str(r.categoryKey())
                            + ",\"categoryLabel\":" + Json.str(r.categoryLabel())
                            + ",\"available\":" + Json.num(r.available())
                            + ",\"fromEnvelope\":" + Json.num(r.fromEnvelope())
                            + ",\"fromMain\":" + Json.num(r.fromMain()) + "}");
                }
                default -> send(ex, 404, "application/json", "{\"error\":\"not found\"}");
            }
        } catch (IllegalArgumentException e) {
            send(ex, 400, "application/json", "{\"error\":" + Json.str(String.valueOf(e.getMessage())) + "}");
        }
    }

    // ------------------------------------------------------------------
    //  Fichiers statiques (dashboard)
    // ------------------------------------------------------------------

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) path = "/index.html";

        // Protection basique contre la traversee de repertoire.
        Path file = WEB_DIR.resolve(path.substring(1)).normalize();
        if (!file.startsWith(WEB_DIR.toAbsolutePath().normalize())
                && !file.startsWith(WEB_DIR.normalize())) {
            send(ex, 403, "text/plain", "Forbidden");
            return;
        }

        if (!Files.exists(file) || Files.isDirectory(file)) {
            send(ex, 404, "text/plain", "404 - " + path + " introuvable");
            return;
        }

        byte[] body = Files.readAllBytes(file);
        send(ex, 200, contentType(file.toString()), body);
    }

    // ------------------------------------------------------------------
    //  Helpers HTTP
    // ------------------------------------------------------------------

    /** Extrait les budgets surcharges depuis la query : cle = nom de categorie (ex. LOISIRS), valeur = montant. */
    private static Map<String, Double> budgetOverrides(Map<String, String> q) {
        Map<String, Double> out = new HashMap<>();
        for (com.the404squad.model.Category c : com.the404squad.model.Category.values()) {
            String v = q.get(c.name());
            if (v != null) {
                try { out.put(c.name(), Double.parseDouble(v)); }
                catch (NumberFormatException ignore) { /* valeur invalide -> budget par defaut */ }
            }
        }
        return out;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isBlank()) return map;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(pair.substring(0, eq),
                        java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (name.endsWith(".svg"))  return "image/svg+xml";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        return "application/octet-stream";
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        send(ex, status, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
