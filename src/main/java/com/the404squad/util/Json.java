package com.the404squad.util;

/** Mini-aide pour construire du JSON a la main (aucune dependance externe). */
public final class Json {

    private Json() {}

    /** Echappe une chaine pour l'inclure dans du JSON. */
    public static String str(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** Formate un nombre avec 2 decimales et un point decimal (JSON-safe). */
    public static String num(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }
}
