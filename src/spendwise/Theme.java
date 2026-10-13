package spendwise;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Colors, fonts and number formatting shared by the whole UI. */
public final class Theme {
    private Theme() {}

    public static final Color BG       = new Color(0x0B1120);
    public static final Color SIDEBAR  = new Color(0x070C18);
    public static final Color CARD     = new Color(0x131C2E);
    public static final Color CARD_ALT = new Color(0x182339);
    public static final Color BORDER   = new Color(0x22304A);
    public static final Color TEXT     = new Color(0xF1F5F9);
    public static final Color MUTED    = new Color(0x8B9AB5);
    public static final Color GREEN    = new Color(0x10B981);
    public static final Color RED      = new Color(0xF43F5E);
    public static final Color BLUE     = new Color(0x38BDF8);
    public static final Color AMBER    = new Color(0xF59E0B);
    public static final Color VIOLET   = new Color(0x8B5CF6);

    public static final Color[] PALETTE = {
            new Color(0x10B981), new Color(0x38BDF8), new Color(0xF59E0B),
            new Color(0x8B5CF6), new Color(0xF43F5E), new Color(0x14B8A6),
            new Color(0xEC4899), new Color(0x84CC16)
    };

    /** Currency symbol, changed from Settings. */
    public static String currency = "₹";

    private static String family;

    public static Font font(int style, float size) {
        if (family == null) {
            Set<String> have = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            family = "SansSerif";
            for (String f : new String[]{"Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Roboto", "Ubuntu", "Poppins"}) {
                if (have.contains(f)) { family = f; break; }
            }
        }
        return new Font(family, style, 1).deriveFont(style, size);
    }

    public static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    public static Color categoryColor(String category) {
        if (Transaction.LOAN_CATEGORY.equals(category)) return AMBER;
        return PALETTE[Math.floorMod(category.toLowerCase().hashCode(), PALETTE.length)];
    }

    /** Formats like "₹ 12,500" or "₹ 99.50". Rupee amounts use Indian digit grouping (12,34,567). */
    public static String money(double v) {
        boolean whole = Math.abs(v - Math.rint(v)) < 0.005;
        String plain = whole ? String.valueOf((long) Math.abs(Math.rint(v)))
                             : String.format(Locale.ROOT, "%.2f", Math.abs(v));
        int dot = plain.indexOf('.');
        String intPart = dot < 0 ? plain : plain.substring(0, dot);
        String frac = dot < 0 ? "" : plain.substring(dot);
        boolean indian = currency.equals("₹") || currency.equalsIgnoreCase("Rs") || currency.equalsIgnoreCase("INR");
        boolean negative = v < 0 && !(whole && Math.rint(v) == 0);
        return (negative ? "-" : "") + currency + " " + group(intPart, indian) + frac;
    }

    static String group(String digits, boolean indian) {
        int n = digits.length();
        if (n <= 3) return digits;
        StringBuilder sb = new StringBuilder();
        String head = digits.substring(0, n - 3), tail = digits.substring(n - 3);
        int size = indian ? 2 : 3;
        for (int i = 0; i < head.length(); i++) {
            if (i > 0 && (head.length() - i) % size == 0) sb.append(',');
            sb.append(head.charAt(i));
        }
        return sb.append(',').append(tail).toString();
    }

    /** Short form for chart axes: 1.5K, 2.3L, 1.1Cr. */
    public static String compact(double v) {
        double a = Math.abs(v);
        String s;
        if (a >= 1e7) s = trim(a / 1e7) + "Cr";
        else if (a >= 1e5) s = trim(a / 1e5) + "L";
        else if (a >= 1e3) s = trim(a / 1e3) + "K";
        else s = trim(a);
        return (v < 0 ? "-" : "") + s;
    }

    private static String trim(double d) {
        String s = String.format(Locale.ROOT, "%.1f", d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
