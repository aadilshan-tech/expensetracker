package spendwise;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/** Custom-painted charts driven by real data. */
public final class Charts {
    private Charts() {}

    private static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static void emptyState(Graphics2D g, int w, int h, String msg) {
        g.setFont(Theme.font(Font.PLAIN, 13f));
        g.setColor(Theme.MUTED);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    /** Rounds a maximum up to a "nice" axis value (1, 2, 5 x 10^n). */
    static double niceMax(double v) {
        if (v <= 0) return 1;
        double exp = Math.pow(10, Math.floor(Math.log10(v)));
        double f = v / exp;
        double nice = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return nice * exp;
    }

    // ------------------------------------------------------------------ grouped bars

    public static class CashFlowChart extends JComponent {
        private String[] labels = new String[0];
        private double[] income = new double[0], expense = new double[0];

        public CashFlowChart() { setOpaque(false); setPreferredSize(new Dimension(300, 220)); }

        public void setData(String[] labels, double[] income, double[] expense) {
            this.labels = labels; this.income = income; this.expense = expense;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            double max = 0;
            for (int i = 0; i < labels.length; i++) max = Math.max(max, Math.max(income[i], expense[i]));
            if (max <= 0) { emptyState(g, w, h, "No income or expenses recorded yet"); g.dispose(); return; }
            max = niceMax(max);

            int left = 50, right = 8, top = 30, bottom = 26;
            int pw = w - left - right, ph = h - top - bottom;
            Font small = Theme.font(Font.PLAIN, 11f);
            g.setFont(small);
            FontMetrics fm = g.getFontMetrics();

            // legend
            int lx = w - right;
            for (Object[] item : new Object[][]{{"Expenses", Theme.RED}, {"Income", Theme.GREEN}}) {
                String t = (String) item[0];
                lx -= fm.stringWidth(t);
                g.setColor(Theme.MUTED);
                g.drawString(t, lx, 12);
                lx -= 14;
                g.setColor((Color) item[1]);
                g.fill(new Ellipse2D.Double(lx, 4, 8, 8));
                lx -= 16;
            }

            // grid + y labels
            for (int i = 0; i <= 4; i++) {
                double v = max * i / 4;
                int y = top + ph - (int) Math.round(ph * i / 4.0);
                g.setColor(Theme.alpha(Theme.BORDER, i == 0 ? 255 : 150));
                g.setStroke(new BasicStroke(1f));
                g.drawLine(left, y, left + pw, y);
                g.setColor(Theme.MUTED);
                String s = Theme.compact(v);
                g.drawString(s, left - 8 - fm.stringWidth(s), y + 4);
            }

            int n = labels.length;
            double gw = (double) pw / n;
            double bw = Math.min(22, gw * 0.3);
            for (int i = 0; i < n; i++) {
                double cx = left + gw * i + gw / 2;
                bar(g, cx - bw - 2, bw, income[i], max, top, ph, Theme.GREEN);
                bar(g, cx + 2, bw, expense[i], max, top, ph, Theme.RED);
                g.setColor(Theme.MUTED);
                g.setFont(small);
                g.drawString(labels[i], (int) (cx - fm.stringWidth(labels[i]) / 2.0), h - 8);
            }
            g.dispose();
        }

        private void bar(Graphics2D g, double x, double bw, double v, double max, int top, int ph, Color c) {
            if (v <= 0) return;
            double bh = Math.max(3, ph * v / max);
            double y = top + ph - bh;
            g.setPaint(new GradientPaint(0, (float) y, c, 0, (float) (y + bh), Theme.alpha(c, 120)));
            Path2D p = new Path2D.Double();
            double r = Math.min(6, bw / 2), b = top + ph;
            p.moveTo(x, b); p.lineTo(x, y + r);
            p.quadTo(x, y, x + r, y); p.lineTo(x + bw - r, y);
            p.quadTo(x + bw, y, x + bw, y + r); p.lineTo(x + bw, b);
            p.closePath();
            g.fill(p);
        }
    }

    // ------------------------------------------------------------------ donut + legend

    public static class DonutChart extends JComponent {
        private LinkedHashMap<String, Double> data = new LinkedHashMap<>();
        private String centerLabel = "Spent";
        private int maxItems = 5;
        private boolean legend = true;

        public DonutChart() { setOpaque(false); setPreferredSize(new Dimension(300, 220)); }

        public DonutChart legend(boolean on) { this.legend = on; return this; }

        public void setData(Map<String, Double> raw, String centerLabel) {
            // keep the biggest slices, fold the rest into "Other"
            LinkedHashMap<String, Double> d = new LinkedHashMap<>();
            double other = 0; int i = 0;
            for (Map.Entry<String, Double> e : raw.entrySet()) {
                if (i++ < maxItems) d.put(e.getKey(), e.getValue()); else other += e.getValue();
            }
            if (other > 0) d.put("Other", other);
            this.data = d;
            this.centerLabel = centerLabel;
            repaint();
        }

        static Color colorFor(String name, int index) {
            return "Other".equals(name) ? Theme.MUTED : Theme.PALETTE[index % Theme.PALETTE.length];
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            double total = 0;
            for (double v : data.values()) total += v;
            if (total <= 0) { emptyState(g, w, h, "No spending to show"); g.dispose(); return; }

            int d = legend ? Math.min(h - 8, (int) (w * 0.5)) : Math.min(h - 8, w - 8);
            d = Math.max(60, d);
            double thick = Math.max(14, d * 0.16);
            double ox = legend ? 4 : (w - d) / 2.0, oy = (h - d) / 2.0;
            Arc2D.Double arc = new Arc2D.Double(ox + thick / 2, oy + thick / 2, d - thick, d - thick, 90, 0, Arc2D.OPEN);

            double start = 90;
            int idx = 0;
            boolean gaps = data.size() > 1;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                double sweep = -360.0 * e.getValue() / total;
                arc.setAngleStart(start);
                arc.setAngleExtent(gaps ? sweep + 2 : sweep);
                g.setColor(colorFor(e.getKey(), idx++));
                g.setStroke(new BasicStroke((float) thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                g.draw(arc);
                start += sweep;
            }

            // centre text
            double cx = ox + d / 2.0, cy = oy + d / 2.0;
            g.setFont(Theme.font(Font.BOLD, Math.max(13f, d * 0.12f)));
            String big = Theme.currency + " " + Theme.compact(total);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.TEXT);
            g.drawString(big, (float) (cx - fm.stringWidth(big) / 2.0), (float) (cy + 2));
            g.setFont(Theme.font(Font.PLAIN, 11.5f));
            fm = g.getFontMetrics();
            g.setColor(Theme.MUTED);
            g.drawString(centerLabel, (float) (cx - fm.stringWidth(centerLabel) / 2.0), (float) (cy + 18));

            // legend
            if (legend) {
                int x = (int) (ox + d + 22), rowH = 28;
                int y = (h - rowH * data.size()) / 2 + 12;
                idx = 0;
                for (Map.Entry<String, Double> e : data.entrySet()) {
                    g.setColor(colorFor(e.getKey(), idx++));
                    g.fill(new RoundRectangle2D.Double(x, y - 9, 10, 10, 4, 4));
                    g.setFont(Theme.font(Font.PLAIN, 13f));
                    g.setColor(Theme.TEXT);
                    String name = e.getKey();
                    int maxW = w - x - 62;
                    FontMetrics f2 = g.getFontMetrics();
                    while (f2.stringWidth(name) > maxW && name.length() > 3) name = name.substring(0, name.length() - 2);
                    if (!name.equals(e.getKey())) name += "…";
                    g.drawString(name, x + 18, y);
                    String pct = String.format(Locale.ROOT, "%.0f%%", 100 * e.getValue() / total);
                    g.setColor(Theme.MUTED);
                    g.drawString(pct, w - 6 - f2.stringWidth(pct), y);
                    y += rowH;
                }
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ horizontal progress bar

    public static class ProgressBar extends JComponent {
        private double value; // 0..1
        private Color color;
        public ProgressBar(Color color) { this.color = color; setOpaque(false); setPreferredSize(new Dimension(100, 10)); }
        public void set(double v, Color c) { value = Math.max(0, Math.min(1, v)); color = c; repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int h = Math.min(10, getHeight());
            int y = (getHeight() - h) / 2;
            g.setColor(Theme.alpha(Color.WHITE, 18));
            g.fill(new RoundRectangle2D.Double(0, y, getWidth(), h, h, h));
            if (value > 0) {
                g.setColor(color);
                g.fill(new RoundRectangle2D.Double(0, y, Math.max(h, getWidth() * value), h, h, h));
            }
            g.dispose();
        }
    }
}
