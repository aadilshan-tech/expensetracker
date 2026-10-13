package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

public class ReportsScreen extends Screen {
    private final JLabel subtitle = Ui.muted(" ");
    private final JComboBox<Integer> year = new JComboBox<>();
    private final StatCard incCard = new StatCard("Income", Theme.BLUE);
    private final StatCard expCard = new StatCard("Expenses", Theme.RED);
    private final StatCard netCard = new StatCard("Net savings", Theme.GREEN);
    private final StatCard avgCard = new StatCard("Avg. monthly spend", Theme.VIOLET);
    private final Charts.CashFlowChart chart = new Charts.CashFlowChart();
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private boolean updating;

    public ReportsScreen(MainFrame app, DataStore store) {
        super(app, store);
        year.setFont(Theme.font(Font.PLAIN, 14f));
        year.setPreferredSize(new Dimension(110, 38));
        Ui.Btn export = Ui.secondary("Export CSV");
        export.addActionListener(e -> export());
        add(header("Reports", subtitle, year, export), BorderLayout.NORTH);

        Page page = new Page(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.BOTH;

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(incCard); stats.add(expCard); stats.add(netCard); stats.add(avgCard);
        g.gridy = 0; g.insets = new Insets(0, 0, 18, 0);
        page.add(stats, g);

        Ui.Card chartCard = new Ui.Card();
        chartCard.setLayout(new BorderLayout(0, 8));
        chartCard.add(Ui.label("Income vs expenses", 16f, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(300, 240));
        chartCard.add(chart, BorderLayout.CENTER);
        g.gridy = 1; g.weighty = 0.45;
        page.add(chartCard, g);

        Ui.styleTable(table);
        table.setRowHeight(42);
        Ui.column(table, 0, false, null);
        Ui.column(table, 1, true, v -> Theme.GREEN);
        Ui.column(table, 2, true, v -> Theme.RED);
        Ui.signedColumn(table, 3, v -> (Double) v >= 0 ? Theme.GREEN : Theme.RED);
        Ui.column(table, 4, true, v -> Theme.MUTED);
        table.setRowSelectionAllowed(false);
        Ui.Card tableCard = new Ui.Card();
        tableCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        tableCard.setLayout(new BorderLayout());
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tableCard.add(table.getTableHeader(), BorderLayout.NORTH);
        tableCard.add(table, BorderLayout.CENTER);
        g.gridy = 2; g.weighty = 0; g.insets = new Insets(18, 0, 0, 0);
        page.add(tableCard, g);

        add(Ui.scroll(page), BorderLayout.CENTER);
        year.addActionListener(e -> { if (!updating) refresh(); });
    }

    private int selectedYear() {
        Integer y = (Integer) year.getSelectedItem();
        return y == null ? LocalDate.now().getYear() : y;
    }

    @Override public void refresh() {
        int keep = year.getSelectedItem() == null ? LocalDate.now().getYear() : selectedYear();
        TreeSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        years.add(LocalDate.now().getYear());
        for (Transaction t : store.transactions) years.add(t.date.getYear());
        updating = true;
        year.removeAllItems();
        for (int y : years) year.addItem(y);
        year.setSelectedItem(years.contains(keep) ? keep : years.first());
        updating = false;

        int y = selectedYear();
        LocalDate today = LocalDate.now();
        int lastMonth = y == today.getYear() ? today.getMonthValue() : 12;
        String[] labels = new String[12];
        double[] in = new double[12], out = new double[12];
        List<Object[]> rows = new ArrayList<>();
        double ti = 0, te = 0;
        int first = lastMonth;
        for (int m = 12; m >= 1; m--) {
            YearMonth ym = YearMonth.of(y, m);
            labels[m - 1] = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            in[m - 1] = store.income(ym);
            out[m - 1] = store.expense(ym);
            if (m <= lastMonth && (in[m - 1] > 0 || out[m - 1] > 0)) first = m; // first month that has data
        }
        for (int m = 1; m <= 12; m++) {
            if (m >= first && m <= lastMonth) {
                rows.add(new Object[]{Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        in[m - 1], out[m - 1], in[m - 1] - out[m - 1], rate(in[m - 1], out[m - 1])});
                ti += in[m - 1];
                te += out[m - 1];
            }
        }
        rows.add(new Object[]{"Total " + y, ti, te, ti - te, rate(ti, te)});
        model.rows = rows;
        model.fireTableDataChanged();
        chart.setData(labels, in, out);

        incCard.set(Theme.money(ti), "in " + y, Theme.MUTED);
        expCard.set(Theme.money(te), "in " + y, Theme.MUTED);
        netCard.set(Theme.money(ti - te), ti > 0 ? rate(ti, te) + " of income saved" : "No income yet", ti - te < 0 ? Theme.RED : Theme.MUTED);
        int span = lastMonth - first + 1;
        avgCard.set(Theme.money(Math.round(te / Math.max(1, span))), "over " + span + (span == 1 ? " month" : " months"), Theme.MUTED);
        subtitle.setText("Monthly summary for " + y + " (loan money is not counted as income or spending)");
        table.setPreferredSize(new Dimension(table.getWidth(), table.getRowHeight() * rows.size()));
        revalidate();
        repaint();
    }

    private static String rate(double inc, double exp) {
        return inc > 0 ? Math.round((inc - exp) / inc * 100) + "%" : "–";
    }

    private void export() {
        List<String[]> out = new ArrayList<>();
        out.add(new String[]{"Month", "Income", "Expenses", "Net", "Savings rate"});
        for (Object[] r : model.rows)
            out.add(new String[]{(String) r[0], String.valueOf(r[1]), String.valueOf(r[2]), String.valueOf(r[3]), (String) r[4]});
        exportCsv("spendwise-report-" + selectedYear() + ".csv", out);
    }

    static class Model extends AbstractTableModel {
        List<Object[]> rows = new ArrayList<>();
        private final String[] cols = {"Month", "Income", "Expenses", "Net", "Savings rate"};
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) { return c >= 1 && c <= 3 ? Double.class : String.class; }
        public Object getValueAt(int r, int c) { return rows.get(r)[c]; }
    }
}
