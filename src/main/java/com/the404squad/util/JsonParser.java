package com.the404squad.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parseur JSON minimal (descente recursive), sans dependance externe.
 *
 * Types renvoyes :
 *   objet   -> {@link Map}&lt;String,Object&gt; (LinkedHashMap, ordre conserve)
 *   tableau -> {@link List}&lt;Object&gt;
 *   chaine  -> {@link String}
 *   nombre  -> {@link Double}
 *   bool    -> {@link Boolean}
 *   null    -> {@code null}
 *
 * Suffisant pour lire les fichiers data/*.json de l'application.
 */
public final class JsonParser {

    private final String s;
    private int i;

    private JsonParser(String src) { this.s = src; }

    /** Parse une chaine JSON et renvoie l'objet correspondant. */
    public static Object parse(String json) {
        JsonParser p = new JsonParser(json);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i < p.s.length()) {
            throw new RuntimeException("JSON : caracteres en trop a la position " + p.i);
        }
        return v;
    }

    // ---- Helpers de cast pratiques ----
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object o) { return (Map<String, Object>) o; }
    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object o) { return (List<Object>) o; }
    public static String asString(Object o) { return o == null ? null : o.toString(); }
    public static double asDouble(Object o) { return o == null ? 0.0 : ((Number) o).doubleValue(); }
    public static int asInt(Object o) { return o == null ? 0 : (int) Math.round(((Number) o).doubleValue()); }

    // ------------------------------------------------------------------

    private Object value() {
        char c = peek();
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't', 'f' -> bool();
            case 'n' -> nul();
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        ws();
        if (peek() == '}') { i++; return map; }
        while (true) {
            ws();
            String key = string();
            ws();
            expect(':');
            ws();
            map.put(key, value());
            ws();
            char c = next();
            if (c == '}') break;
            if (c != ',') throw err("',' ou '}' attendu");
        }
        return map;
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        expect('[');
        ws();
        if (peek() == ']') { i++; return list; }
        while (true) {
            ws();
            list.add(value());
            ws();
            char c = next();
            if (c == ']') break;
            if (c != ',') throw err("',' ou ']' attendu");
        }
        return list;
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') break;
            if (c == '\\') {
                char e = next();
                switch (e) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/'  -> sb.append('/');
                    case 'b'  -> sb.append('\b');
                    case 'f'  -> sb.append('\f');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case 'u'  -> {
                        String hex = s.substring(i, i + 4);
                        i += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw err("echappement invalide \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Double number() {
        int start = i;
        if (peek() == '-') i++;
        while (i < s.length() && isNumChar(s.charAt(i))) i++;
        return Double.parseDouble(s.substring(start, i));
    }

    private Boolean bool() {
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        throw err("booleen attendu");
    }

    private Object nul() {
        if (s.startsWith("null", i)) { i += 4; return null; }
        throw err("null attendu");
    }

    private static boolean isNumChar(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
    }

    // ---- micro-lexer ----
    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    private char peek() { if (i >= s.length()) throw err("fin de JSON inattendue"); return s.charAt(i); }
    private char next() { if (i >= s.length()) throw err("fin de JSON inattendue"); return s.charAt(i++); }
    private void expect(char c) { if (next() != c) throw err("'" + c + "' attendu"); }
    private RuntimeException err(String msg) { return new RuntimeException("JSON @" + i + " : " + msg); }
}
