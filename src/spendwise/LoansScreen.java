package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class LoansScreen extends Screen {
    private static final String[] FILTERS = {"Active", "Overdue", "Borrowed", "Lent", "Settled", "All"};

    private final JLabel subtitle = Ui.muted(" ");
    private final StatCard owe = new StatCard("You owe", Theme.RED);
    private final StatCard owed = new StatCard("You're owed", Theme.GREEN);
    private final StatCard overdue = new StatCard("Overdue", Theme.AMBER);
    private final StatCard soon = new StatCard("Due soon", Theme.BLUE);
    private final JComboBox<String> filter = Ui.combo(FILTERS);
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final CardLayout switcher = new CardLayout();
    private final JPanel tableArea = new JPanel(switcher);
    private final JLabel emptyText = Ui.label(" ", 14f, Font.PLAIN, Theme.MUTED);
    private final Ui.Btn payBtn = Ui.primary("Record payment");
    private final Ui.Btn editBtn = Ui.secondary("Edit");
    private final Ui.Btn calBtn = Ui.secondary("Add to calendar");
    private final Ui.Btn deleteBtn = Ui.danger("Delete");

    public LoansScreen(MainFrame app, DataStore store) {
        super(app, store);

        Ui.Btn borrow = Ui.secondary("Borrow money");
        borrow.addActionListener(e -> Dialogs.loan(app, store, Loan.Direction.BORROWED));
        Ui.Btn lend = Ui.primary("Lend money");
        lend.addActionListener(e -> Dialogs.loan(app, store, Loan.Direction.LENT));
        add(header("Borrowed & Lent", subtitle, borrow, lend), BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(owe); stats.add(owed); stats.add(overdue); stats.add(soon);

        filter.setPreferredSize(new Dimension(150, 38));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(Ui.muted("Show"));
        left.add(filter);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(calBtn); right.add(editBtn); right.add(deleteBtn); right.add(payBtn);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(18, 0, 14, 0));
        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(stats, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        // table
        Ui.styleTable(table);
        TableRowSorter<Model> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        Ui.pillColumn(table, 1, v -> "Borrowed".equals(v) ? Theme.VIOLET : Theme.BLUE);
        Ui.column(table, 3, true, v -> Theme.MUTED);
        Ui.column(table, 4, true, v -> (Double) v > 0 ? Theme.TEXT : Theme.GREEN);
        Ui.column(table, 5, false, null);
        Ui.pillColumn(table, 6, v -> {
            String s = String.valueOf(v);
            return s.startsWith("Overdue") ? Theme.RED : s.startsWith("Due") ? Theme.AMBER
                    : s.equals("Settled") ? Theme.GREEN : Theme.MUTED;
        });
        int[] widths = {200, 110, 120, 120, 130, 130, 150};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        Ui.Card tableCard = new Ui.Card();
        tableCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        tableCard.setLayout(new BorderLayout());
        JScrollPane sp = Ui.scroll(table);
        sp.getViewport().setBackground(Theme.CARD);
        sp.getViewport().setOpaque(true);

        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        JPanel emptyBox = new JPanel(new GridLayout(0, 1, 0, 12));
        emptyBox.setOpaque(false);
        emptyText.setHorizontalAlignment(SwingConstants.CENTER);
        emptyBox.add(emptyText);
        empty.add(emptyBox);
        tableArea.setOpaque(false);
        tableArea.add(sp, "table");
        tableArea.add(empty, "empty");
        tableCard.add(tableArea, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        JLabel hint = Ui.muted("Reminders show when SpendWise opens and while it is running. Use \"Add to calendar\" to be reminded even when it is closed.");
        hint.setBorder(new EmptyBorder(12, 4, 0, 0));
        center.add(hint, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // behaviour
        filter.addActionListener(e -> refresh());
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        payBtn.addActionListener(e -> { Loan l = selected(); if (l != null) Dialogs.payment(app, store, l); });
        editBtn.addActionListener(e -> { Loan l = selected(); if (l != null) Dialogs.editLoan(app, store, l); });
        calBtn.addActionListener(e -> exportCalendar());
        deleteBtn.addActionListener(e -> deleteSelected());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Loan l = selected();
                if (e.getClickCount() == 2 && l != null && !l.isSettled()) Dialogs.payment(app, store, l);
            }
        });
        updateButtons();
    }

    private Loan selected() {
        int r = table.getSelectedRow();
        return r < 0 ? null : model.rows.get(table.convertRowIndexToModel(r));
    }

    private void updateButtons() {
        Loan l = selected();
        payBtn.setEnabled(l != null && !l.isSettled());
        editBtn.setEnabled(l != null);
        deleteBtn.setEnabled(l != null);
        calBtn.setText(l != null ? "Add to calendar" : "Calendar (all active)");
        calBtn.setEnabled(l != null ? !l.isSettled() : store.loans.stream().anyMatch(x -> !x.isSettled()));
        if (l != null) payBtn.setText(l.isBorrowed() ? "Pay back" : "Received money");
        else payBtn.setText("Record payment");
    }

    private void deleteSelected() {
        Loan l = selected();
        if (l == null) return;
        String extra = l.linked ? "\nIts entries in your balance will be removed too." : "";
        if (Ui.confirm(this, "Delete loan",
                "Delete the " + (l.isBorrowed() ? "borrowing from " : "loan to ") + l.person + " ("
                        + Theme.money(l.amount) + ")?" + extra, "Delete"))
            store.deleteLoan(l);
    }

    private void exportCalendar() {
        Loan sel = selected();
        List<Loan> list = new ArrayList<>();
        if (sel != null) list.add(sel);
        else for (Loan l : store.loans) if (!l.isSettled()) list.add(l);
        if (list.isEmpty()) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(sel != null ? "payback-" + sel.person.replaceAll("[^A-Za-z0-9]+", "-") + ".ics"
                : "spendwise-loan-reminders.ics"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".ics")) f = new File(f.getParentFile(), f.getName() + ".ics");
        try {
            Files.write(f.toPath(), CalendarExport.build(list).getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this,
                    list.size() + (list.size() == 1 ? " reminder" : " reminders") + " saved to\n" + f.getAbsolutePath()
                            + "\n\nOpen the file (or import it in Google Calendar, Outlook or Apple Calendar).\n"
                            + "You will get an alert at 9 AM the day before and at 9 AM on the due date.",
                    "Calendar file created", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the file:\n" + ex.getMessage(), "Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override public void refresh() {
        LocalDate today = LocalDate.now();
        int lead = store.settings.leadDays;
        double od = 0, ds = 0;
        int odN = 0, dsN = 0, owedN = 0, oweN = 0;
        for (Loan l : store.loans) {
            if (l.isSettled()) continue;
            if (l.isBorrowed()) oweN++; else owedN++;
            Loan.Status s = l.status(today, lead);
            if (s == Loan.Status.OVERDUE) { odN++; od += l.remaining(); }
            if (s == Loan.Status.DUE_TODAY || s == Loan.Status.DUE_SOON) { dsN++; ds += l.remaining(); }
        }
        owe.set(Theme.money(store.totalOwed()), oweN + (oweN == 1 ? " open borrowing" : " open borrowings"), Theme.MUTED);
        owed.set(Theme.money(store.totalReceivable()), owedN + (owedN == 1 ? " open loan" : " open loans"), Theme.MUTED);
        overdue.set(String.valueOf(odN), odN == 0 ? "Nothing late" : Theme.money(od) + " late", odN == 0 ? Theme.MUTED : Theme.RED);
        soon.set(String.valueOf(dsN), dsN == 0 ? "Next " + lead + " days are clear" : Theme.money(ds) + " in the next " + lead + " days", Theme.MUTED);
        subtitle.setText("Net: " + Theme.money(store.totalReceivable() - store.totalOwed())
                + (store.totalReceivable() >= store.totalOwed() ? " in your favour" : " you owe overall"));

        String f = (String) filter.getSelectedItem();
        List<Loan> rows = new ArrayList<>();
        for (Loan l : store.loans) {
            boolean settled = l.isSettled();
            boolean keep;
            switch (f == null ? "Active" : f) {
                case "Overdue":  keep = !settled && l.status(today, lead) == Loan.Status.OVERDUE; break;
                case "Borrowed": keep = !settled && l.isBorrowed(); break;
                case "Lent":     keep = !settled && !l.isBorrowed(); break;
                case "Settled":  keep = settled; break;
                case "All":      keep = true; break;
                default:         keep = !settled;
            }
            if (keep) rows.add(l);
        }
        rows.sort(Comparator.comparing((Loan l) -> l.isSettled()).thenComparing(l -> l.dueDate));
        model.rows = rows;
        model.today = today;
        model.lead = lead;
        model.fireTableDataChanged();

        if (rows.isEmpty()) {
            emptyText.setText(store.loans.isEmpty()
                    ? "No borrowed or lent money yet. Use the buttons above to add one."
                    : "Nothing matches this filter.");
            switcher.show(tableArea, "empty");
        } else switcher.show(tableArea, "table");
        updateButtons();
    }

    static class Model extends AbstractTableModel {
        List<Loan> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int lead = 3;
        private final String[] cols = {"Person", "Type", "Amount", "Paid", "Remaining", "Due date", "Status"};
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) {
            return c >= 2 && c <= 4 ? Double.class : c == 5 ? LocalDate.class : String.class;
        }
        public Object getValueAt(int r, int c) {
            Loan l = rows.get(r);
            switch (c) {
                case 0: return l.person;
                case 1: return l.isBorrowed() ? "Borrowed" : "Lent";
                case 2: return l.amount;
                case 3: return l.paid;
                case 4: return l.remaining();
                case 5: return l.dueDate;
                default: return Loan.statusText(l, today, lead);
            }
        }
    }
}
