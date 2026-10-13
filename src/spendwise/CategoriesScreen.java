package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class CategoriesScreen extends Screen {
    private static final String[] PERIODS = {"This month", "Last 30 days", "This year", "All time"};

    private final JLabel subtitle = Ui.muted(" ");
    private final JComboBox<String> period = Ui.combo(PERIODS);
    private final Charts.DonutChart donut = new Charts.DonutChart().legend(false);
    private final JPanel list = new JPanel();

    public CategoriesScreen(MainFrame app, DataStore store) {
        super(app, store);
        period.setPreferredSize(new Dimension(170, 38));
        add(header("Categories", subtitle, period), BorderLayout.NORTH);

        Ui.Card donutCard = new Ui.Card();
        donutCard.setLayout(new BorderLayout(0, 8));
        donutCard.add(Ui.label("Where your money goes", 16f, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        donutCard.add(donut, BorderLayout.CENTER);

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        Page listPage = new Page(new BorderLayout());
        listPage.add(list, BorderLayout.NORTH);
        Ui.Card listCard = new Ui.Card();
        listCard.setLayout(new BorderLayout(0, 8));
        listCard.add(Ui.label("Breakdown", 16f, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        listCard.add(Ui.scroll(listPage), BorderLayout.CENTER);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        c.gridx = 0; c.weightx = 0.4; c.insets = new Insets(0, 0, 0, 16);
        row.add(donutCard, c);
        c.gridx = 1; c.weightx = 0.6; c.insets = new Insets(0, 0, 0, 0);
        row.add(listCard, c);
        add(row, BorderLayout.CENTER);

        period.addActionListener(e -> refresh());
    }

    @Override public void refresh() {
        LocalDate today = LocalDate.now(), from;
        switch ((String) period.getSelectedItem()) {
            case "This month":   from = today.withDayOfMonth(1); break;
            case "Last 30 days": from = today.minusDays(29); break;
            case "This year":    from = today.withDayOfYear(1); break;
            default:             from = null;
        }
        LinkedHashMap<String, Double> data = store.expenseByCategory(from, today);
        Map<String, Integer> counts = new HashMap<>();
        for (Transaction t : store.transactions) {
            if (t.isLoanRelated() || t.isIncome()) continue;
            if (from != null && t.date.isBefore(from)) continue;
            if (t.date.isAfter(today)) continue;
            counts.merge(t.category, 1, Integer::sum);
        }
        double total = 0;
        for (double v : data.values()) total += v;

        donut.setData(data, (String) period.getSelectedItem());
        subtitle.setText(data.isEmpty() ? "No spending in this period"
                : "Top category: " + data.keySet().iterator().next() + " ("
                + Math.round(100 * data.values().iterator().next() / total) + "% of " + Theme.money(total) + ")");

        list.removeAll();
        if (data.isEmpty()) {
            JLabel l = Ui.muted("Add some expenses to see them broken down here.");
            l.setBorder(new EmptyBorder(30, 0, 0, 0));
            list.add(l);
        }
        for (Map.Entry<String, Double> e : data.entrySet()) {
            double share = total > 0 ? e.getValue() / total : 0;
            Color color = Theme.categoryColor(e.getKey());
            JPanel row = new JPanel(new BorderLayout(0, 8));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(10, 0, 10, 0));

            JPanel top = new JPanel(new BorderLayout(12, 0));
            top.setOpaque(false);
            top.add(new Dot(color), BorderLayout.WEST);
            JPanel names = new JPanel(new GridLayout(2, 1, 0, 1));
            names.setOpaque(false);
            names.add(Ui.label(e.getKey(), 14f, Font.BOLD, Theme.TEXT));
            int n = counts.getOrDefault(e.getKey(), 0);
            names.add(Ui.muted(n + (n == 1 ? " transaction" : " transactions")));
            top.add(names, BorderLayout.CENTER);
            JPanel amt = new JPanel(new GridLayout(2, 1, 0, 1));
            amt.setOpaque(false);
            JLabel a = Ui.label(Theme.money(e.getValue()), 14f, Font.BOLD, Theme.TEXT);
            JLabel p = Ui.muted(Math.round(share * 100) + "%");
            a.setHorizontalAlignment(SwingConstants.RIGHT);
            p.setHorizontalAlignment(SwingConstants.RIGHT);
            amt.add(a); amt.add(p);
            top.add(amt, BorderLayout.EAST);

            Charts.ProgressBar bar = new Charts.ProgressBar(color);
            bar.set(share, color);
            row.add(top, BorderLayout.NORTH);
            row.add(bar, BorderLayout.SOUTH);
            list.add(row);
        }
        revalidate();
        repaint();
    }

    static class Dot extends JComponent {
        private final Color c;
        Dot(Color c) { this.c = c; setPreferredSize(new Dimension(14, 14)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(c);
            g.fill(new Ellipse2D.Double(1, (getHeight() - 12) / 2.0, 12, 12));
            g.dispose();
        }
    }
}
