package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class TransactionsScreen extends Screen {
    private static final String ALL_TYPES = "All types", ALL_CATS = "All categories";

    private final JLabel subtitle = Ui.muted(" ");
    private final JTextField search = Ui.field("Search description or category");
    private final JComboBox<String> typeBox = Ui.combo(new String[]{ALL_TYPES, Transaction.INCOME, Transaction.EXPENSE});
    private final JComboBox<String> catBox = Ui.combo(new String[]{ALL_CATS});
    private final JTable table;
    private final Model model = new Model();
    private final JLabel footer = Ui.muted(" ");
    private final Ui.Btn editBtn = Ui.secondary("Edit");
    private final Ui.Btn deleteBtn = Ui.danger("Delete");
    private boolean updatingCats;

    public TransactionsScreen(MainFrame app, DataStore store) {
        super(app, store);

        Ui.Btn add = Ui.primary("+  Add transaction");
        add.addActionListener(e -> Dialogs.transaction(app, store, null));
        Ui.Btn export = Ui.secondary("Export CSV");
        export.addActionListener(e -> export());
        add(header("Transactions", subtitle, export, add), BorderLayout.NORTH);

        // toolbar
        search.setPreferredSize(new Dimension(280, 38));
        typeBox.setPreferredSize(new Dimension(150, 38));
        catBox.setPreferredSize(new Dimension(170, 38));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setOpaque(false);
        filters.add(search); filters.add(typeBox); filters.add(catBox);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(editBtn); actions.add(deleteBtn);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 14, 0));
        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);

        table = new JTable(model);
        Ui.styleTable(table);
        TableRowSorter<Model> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        Ui.column(table, 0, false, v -> Theme.MUTED);
        Ui.pillColumn(table, 3, v -> Transaction.INCOME.equals(v) ? Theme.GREEN : Theme.MUTED);
        Ui.signedColumn(table, 4, v -> (Double) v >= 0 ? Theme.GREEN : Theme.RED);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);

        Ui.Card tableCard = new Ui.Card();
        tableCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        tableCard.setLayout(new BorderLayout());
        JScrollPane sp = Ui.scroll(table);
        sp.getViewport().setBackground(Theme.CARD);
        sp.getViewport().setOpaque(true);
        tableCard.add(sp, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        footer.setBorder(new EmptyBorder(12, 4, 0, 0));
        center.add(footer, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // behaviour
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        };
        search.getDocument().addDocumentListener(dl);
        typeBox.addActionListener(e -> applyFilter());
        catBox.addActionListener(e -> { if (!updatingCats) applyFilter(); });
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) editSelected(); }
        });
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "del");
        table.getActionMap().put("del", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { deleteSelected(); }
        });
        updateButtons();
    }

    private Transaction selected() {
        int r = table.getSelectedRow();
        return r < 0 ? null : model.rows.get(table.convertRowIndexToModel(r));
    }

    private void updateButtons() {
        boolean on = selected() != null;
        editBtn.setEnabled(on);
        deleteBtn.setEnabled(on);
    }

    private boolean blockedLoanRow(Transaction t) {
        if (!t.loanId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "This entry belongs to a loan, so it is managed from the Loans page.\n"
                            + "Delete or edit the loan there and its entries stay in sync.",
                    "Managed by Loans", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }

    private void editSelected() {
        Transaction t = selected();
        if (t == null || blockedLoanRow(t)) return;
        Dialogs.transaction(app, store, t);
    }

    private void deleteSelected() {
        Transaction t = selected();
        if (t == null || blockedLoanRow(t)) return;
        if (Ui.confirm(this, "Delete transaction", "Delete \"" + t.description + "\" (" + Theme.money(t.amount) + ")?", "Delete"))
            store.deleteTransaction(t);
    }

    private void export() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Date", "Description", "Category", "Type", "Amount"});
        for (Transaction t : model.rows)
            rows.add(new String[]{t.date.toString(), t.description, t.category, t.type, String.valueOf(t.amount)});
        exportCsv("spendwise-transactions.csv", rows);
    }

    @SuppressWarnings("unchecked")
    private void applyFilter() {
        List<RowFilter<Model, Integer>> fs = new ArrayList<>();
        String q = search.getText().trim();
        if (!q.isEmpty()) fs.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q), 1, 2));
        String type = (String) typeBox.getSelectedItem();
        if (type != null && !ALL_TYPES.equals(type)) fs.add(RowFilter.regexFilter("^" + type + "$", 3));
        String cat = (String) catBox.getSelectedItem();
        if (cat != null && !ALL_CATS.equals(cat)) fs.add(RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(cat) + "$", 2));
        ((TableRowSorter<Model>) table.getRowSorter()).setRowFilter(fs.isEmpty() ? null : RowFilter.andFilter(fs));
        updateFooter();
        updateButtons();
    }

    private void updateFooter() {
        double inc = 0, exp = 0;
        int n = table.getRowCount();
        for (int i = 0; i < n; i++) {
            Transaction t = model.rows.get(table.convertRowIndexToModel(i));
            if (t.isLoanRelated()) continue;
            if (t.isIncome()) inc += t.amount; else exp += t.amount;
        }
        footer.setText(n + (n == 1 ? " transaction" : " transactions") + "   ·   Income " + Theme.money(inc)
                + "   ·   Expenses " + Theme.money(exp) + "   ·   loan money is not counted");
    }

    @Override public void refresh() {
        // keep the category filter in sync with the data
        Object keep = catBox.getSelectedItem();
        updatingCats = true;
        catBox.removeAllItems();
        catBox.addItem(ALL_CATS);
        for (String c : store.categories()) catBox.addItem(c);
        catBox.addItem(Transaction.LOAN_CATEGORY);
        catBox.setSelectedItem(keep);
        if (catBox.getSelectedItem() == null) catBox.setSelectedIndex(0);
        updatingCats = false;

        model.rows = new ArrayList<>(store.transactions);
        Collections.reverse(model.rows); // newest first among equal dates
        model.fireTableDataChanged();
        subtitle.setText(store.transactions.size() + " entries recorded");
        updateFooter();
        updateButtons();
    }

    static class Model extends AbstractTableModel {
        List<Transaction> rows = new ArrayList<>();
        private final String[] cols = {"Date", "Description", "Category", "Type", "Amount"};
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) {
            return c == 0 ? java.time.LocalDate.class : c == 4 ? Double.class : String.class;
        }
        public Object getValueAt(int r, int c) {
            Transaction t = rows.get(r);
            switch (c) {
                case 0: return t.date;
                case 1: return t.description;
                case 2: return t.category;
                case 3: return t.type;
                default: return t.isIncome() ? t.amount : -t.amount;
            }
        }
    }
}
