package spendwise;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Small vector icons drawn with Java2D (no image files or emoji fonts needed). */
public final class Icons {
    private Icons() {}

    public static Icon nav(String key, Color color) {
        return new Icon() {
            public int getIconWidth() { return 20; }
            public int getIconHeight() { return 20; }
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g.setColor(color);
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (key) {
                    case "Dashboard":
                        g.draw(new RoundRectangle2D.Double(2, 2, 6.5, 6.5, 2, 2));
                        g.draw(new RoundRectangle2D.Double(11.5, 2, 6.5, 6.5, 2, 2));
                        g.draw(new RoundRectangle2D.Double(2, 11.5, 6.5, 6.5, 2, 2));
                        g.draw(new RoundRectangle2D.Double(11.5, 11.5, 6.5, 6.5, 2, 2));
                        break;
                    case "Add":
                        g.draw(new Ellipse2D.Double(2, 2, 16, 16));
                        g.draw(new Line2D.Double(10, 6, 10, 14));
                        g.draw(new Line2D.Double(6, 10, 14, 10));
                        break;
                    case "Transactions":
                        for (int i = 0; i < 3; i++) {
                            double yy = 4 + i * 6;
                            g.fill(new Ellipse2D.Double(2, yy - 1.3, 2.6, 2.6));
                            g.draw(new Line2D.Double(7, yy, 18, yy));
                        }
                        break;
                    case "Loans":
                        g.draw(new Line2D.Double(3, 7, 17, 7));
                        g.draw(new Polyline(13, 3, 17, 7, 13, 11));
                        g.draw(new Line2D.Double(17, 14, 3, 14));
                        g.draw(new Polyline(7, 10, 3, 14, 7, 18));
                        break;
                    case "Categories":
                        g.draw(new Ellipse2D.Double(2, 2, 16, 16));
                        g.fill(new Arc2D.Double(2, 2, 16, 16, 0, 90, Arc2D.PIE));
                        break;
                    case "Reports":
                        g.fill(new RoundRectangle2D.Double(3, 10, 4, 8, 1.5, 1.5));
                        g.fill(new RoundRectangle2D.Double(8, 5, 4, 13, 1.5, 1.5));
                        g.fill(new RoundRectangle2D.Double(13, 2, 4, 16, 1.5, 1.5));
                        break;
                    case "Goals":
                        g.draw(new Ellipse2D.Double(2, 2, 16, 16));
                        g.draw(new Ellipse2D.Double(6, 6, 8, 8));
                        g.fill(new Ellipse2D.Double(8.5, 8.5, 3, 3));
                        break;
                    case "Settings":
                        double[] ys = {5, 10, 15}, kx = {13, 6, 11};
                        for (int i = 0; i < 3; i++) {
                            g.draw(new Line2D.Double(2, ys[i], 18, ys[i]));
                            g.fill(new Ellipse2D.Double(kx[i] - 2.5, ys[i] - 2.5, 5, 5));
                        }
                        break;
                    default:
                        g.fill(new Ellipse2D.Double(6, 6, 8, 8));
                }
                g.dispose();
            }
        };
    }

    private static class Polyline extends Path2D.Double {
        Polyline(double... p) {
            moveTo(p[0], p[1]);
            for (int i = 2; i < p.length; i += 2) lineTo(p[i], p[i + 1]);
        }
    }
}
