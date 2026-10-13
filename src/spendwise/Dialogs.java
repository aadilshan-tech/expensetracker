package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/** All the pop-up forms of the app. */
public final class Dialogs {
    private Dialogs() {}

    private static final String[] DEFAULT_CATEGORIES = {
            "Food", "Transport", "Fuel", "Shopping", "Bills", "Rent", "Health", "Education",
            "Entertainment", "Salary", "Business", "Gift", "Other"
    };

    static String plain(double v) {
        return Math.abs(v - Math.rint(v)) < 0.005 ? String.valueOf((long) Math.rint(v)) : String.format(Locale.ROOT, "%.2f", v);
    }

    // ------------------------------------------------------------------ income / expense

    /** Add a transaction, or edit one when {@code existing} is not null. */
    public static void transaction(Window owner, DataStore store, Transaction existing) {
        JComboBox<String> type = Ui.combo(new String[]{Transaction.EXPENSE, Transaction.INCOME});
        JTextField amount = Ui.field("0");
        TreeSet<String> cats = new TreeSet<>(Arrays.asList(DEFAULT_CATEGORIES));
        cats.addAll(store.categories());
        JComboBox<String> category = Ui.combo(cats.toArray(new String[0]));
        category.setEditable(true);
        JTextField desc = Ui.field("e.g. Lunch with friends");
        Ui.DateField date = new Ui.DateField(LocalDate.now());

        if (existing != null) {
            type.setSelectedItem(existing.type);
            amount.setText(plain(existing.amount));
            category.setSelectedItem(existing.category);
            desc.setText(existing.description);
            date.setDate(existing.date);
        } else {
            category.setSelectedItem("Food");
        }

        Ui.Form form = new Ui.Form()
                .row("Type", type).row("Amount (" + Theme.currency + ")", amount)
                .row("Category", category).row("Description", desc).row("Date", date);

        Ui.dialog(owner, existing == null ? "Add transaction" : "Edit transaction",
                "Record an expense or income", form, existing == null ? "Add" : "Save", () -> {
                    double a = Ui.parseAmount(amount.getText());
                    if (Double.isNaN(a) || a <= 0) return "Enter an amount greater than 0.";
                    Object sel = category.getEditor().getItem();
                    String cat = sel == null ? "" : sel.toString().trim();
                    if (cat.isEmpty()) return "Choose or type a category.";
                    if (cat.equalsIgnoreCase(Transaction.LOAN_CATEGORY))
                        return "\"Loan\" is reserved. Use the Loans page to borrow or lend.";
                    String d = desc.getText().trim();
                    if (d.isEmpty()) d = cat;
                    if (existing == null) {
                        store.addTransaction(new Transaction(date.getDate(), cat, d, a, (String) type.getSelectedItem()));
                    } else {
                        existing.type = (String) type.getSelectedItem();
                        existing.amount = a;
                        existing.category = cat;
                        existing.description = d;
                        existing.date = date.getDate();
                        store.changed();
                    }
                    return null;
                });
    }

    // ------------------------------------------------------------------ loans

    /** Borrow (you owe) or lend (you are owed). A payback date is always required. */
    public static void loan(Window owner, DataStore store, Loan.Direction dir) {
        boolean borrow = dir == Loan.Direction.BORROWED;
        JTextField person = Ui.field(borrow ? "Who did you borrow from?" : "Who did you lend to?");
        JTextField amount = Ui.field("0");
        Ui.DateField start = new Ui.DateField(LocalDate.now());
        Ui.DateField due = new Ui.DateField(LocalDate.now().plusDays(30));
        JTextField note = Ui.field("Optional (e.g. for rent, medical…)");
        JCheckBox linked = new JCheckBox(borrow ? "Add the borrowed money to my balance" : "Deduct the lent money from my balance", true);
        linked.setOpaque(false);
        linked.setForeground(Theme.TEXT);
        linked.setFont(Theme.font(Font.PLAIN, 13f));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.add(due);
        String[] names = {"1 wk", "2 wks", "1 mo", "3 mo"};
        int[] days = {7, 14, 30, 90};
        for (int i = 0; i < names.length; i++) {
            int dd = days[i];
            Ui.Btn b = Ui.secondary("+" + names[i]);
            b.setBorder(new EmptyBorder(8, 10, 8, 10));
            b.addActionListener(e -> due.setDate(start.getDate().plusDays(dd)));
            chips.add(b);
        }

        Ui.Form form = new Ui.Form()
                .row(borrow ? "Borrowed from" : "Lent to", person)
                .row("Amount (" + Theme.currency + ")", amount)
                .row(borrow ? "Date borrowed" : "Date lent", start)
                .row(borrow ? "Payback date (you'll be reminded)" : "Expected return date (you'll be reminded)", chips)
                .row("Note", note);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 20; g.anchor = GridBagConstraints.WEST;
        form.add(linked, g);

        Ui.dialog(owner, borrow ? "Borrow money" : "Lend money",
                borrow ? "Money you owe someone. Set when you will pay it back."
                       : "Money someone owes you. Set when you expect it back.",
                form, borrow ? "Save borrowing" : "Save lending", () -> {
                    String p = person.getText().trim();
                    if (p.isEmpty()) return "Enter the person's name.";
                    double a = Ui.parseAmount(amount.getText());
                    if (Double.isNaN(a) || a <= 0) return "Enter an amount greater than 0.";
                    LocalDate s = start.getDate(), d = due.getDate();
                    if (d.isBefore(s)) return "The payback date can't be before the " + (borrow ? "borrow" : "lend") + " date.";
                    store.addLoan(Loan.create(p, dir, a, s, d, note.getText().trim(), linked.isSelected()));
                    return null;
                });
    }

    /** Change person, due date or note of an existing loan. */
    public static void editLoan(Window owner, DataStore store, Loan loan) {
        JTextField person = Ui.field("Name");
        person.setText(loan.person);
        Ui.DateField due = new Ui.DateField(loan.dueDate);
        JTextField note = Ui.field("Optional");
        note.setText(loan.note);
        Ui.Form form = new Ui.Form().row("Person", person).row("Payback date", due).row("Note", note);
        Ui.dialog(owner, "Edit " + (loan.isBorrowed() ? "borrowing" : "lending"),
                Theme.money(loan.amount) + " · started " + Ui.DATE_FMT.format(loan.startDate), form, "Save", () -> {
                    String p = person.getText().trim();
                    if (p.isEmpty()) return "Enter the person's name.";
                    if (due.getDate().isBefore(loan.startDate)) return "The payback date can't be before the start date.";
                    loan.person = p;
                    loan.dueDate = due.getDate();
                    loan.note = note.getText().trim();
                    store.changed();
                    return null;
                });
    }

    /** Record a full or partial repayment. */
    public static void payment(Window owner, DataStore store, Loan loan) {
        boolean borrow = loan.isBorrowed();
        JTextField amount = Ui.field("0");
        amount.setText(plain(loan.remaining()));
        Ui.DateField date = new Ui.DateField(LocalDate.now());
        Ui.Form form = new Ui.Form()
                .row(borrow ? "Amount you are paying back" : "Amount you received", amount)
                .row("Date", date);
        Ui.dialog(owner, borrow ? "Record repayment" : "Record money received",
                loan.person + " · " + Theme.money(loan.remaining()) + " remaining of " + Theme.money(loan.amount),
                form, "Record", () -> {
                    double a = Ui.parseAmount(amount.getText());
                    if (Double.isNaN(a) || a <= 0) return "Enter an amount greater than 0.";
                    if (a > loan.remaining() + 0.005) return "That is more than the remaining " + Theme.money(loan.remaining()) + ".";
                    store.recordPayment(loan, a, date.getDate());
                    return null;
                });
    }

    // ------------------------------------------------------------------ goals

    public static void goal(Window owner, DataStore store) {
        JTextField name = Ui.field("e.g. New laptop");
        JTextField target = Ui.field("0");
        JCheckBox useDate = new JCheckBox("Set a target date", false);
        useDate.setOpaque(false);
        useDate.setForeground(Theme.TEXT);
        Ui.DateField date = new Ui.DateField(LocalDate.now().plusMonths(6));
        date.setEnabled(false);
        useDate.addActionListener(e -> date.setEnabled(useDate.isSelected()));
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        dateRow.setOpaque(false);
        dateRow.add(useDate);
        dateRow.add(date);
        Ui.Form form = new Ui.Form().row("Goal name", name).row("Target amount (" + Theme.currency + ")", target).row("Deadline", dateRow);
        Ui.dialog(owner, "New savings goal", "What are you saving for?", form, "Create goal", () -> {
            String n = name.getText().trim();
            if (n.isEmpty()) return "Give your goal a name.";
            double t = Ui.parseAmount(target.getText());
            if (Double.isNaN(t) || t <= 0) return "Enter a target amount greater than 0.";
            store.goals.add(Goal.create(n, t, useDate.isSelected() ? date.getDate() : null));
            store.changed();
            return null;
        });
    }

    public static void goalFunds(Window owner, DataStore store, Goal goal, boolean add) {
        JTextField amount = Ui.field("0");
        Ui.Form form = new Ui.Form().row("Amount (" + Theme.currency + ")", amount);
        Ui.dialog(owner, add ? "Add to " + goal.name : "Withdraw from " + goal.name,
                Theme.money(goal.saved) + " saved of " + Theme.money(goal.target), form, add ? "Add funds" : "Withdraw", () -> {
                    double a = Ui.parseAmount(amount.getText());
                    if (Double.isNaN(a) || a <= 0) return "Enter an amount greater than 0.";
                    if (!add && a > goal.saved + 0.005) return "You only have " + Theme.money(goal.saved) + " saved here.";
                    goal.saved = add ? goal.saved + a : Math.max(0, goal.saved - a);
                    store.changed();
                    return null;
                });
    }

    // ------------------------------------------------------------------ reminders

    public static void reminders(Window owner, DataStore store, Runnable openLoans) {
        LocalDate today = LocalDate.now();
        List<Loan> list = store.pendingReminders(today);
        if (list.isEmpty()) return;
        Screen.Page body = new Screen.Page(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        for (Loan l : list) {
            Loan.Status s = l.status(today, store.settings.leadDays);
            Color c = s == Loan.Status.OVERDUE ? Theme.RED : s == Loan.Status.DUE_TODAY ? Theme.AMBER : Theme.BLUE;
            Ui.Card row = new Ui.Card(14, Theme.CARD_ALT).accent(c);
            row.setLayout(new BorderLayout(10, 2));
            row.setBorder(new EmptyBorder(12, 18, 12, 14));
            JLabel msg = Ui.label("<html><body style='width:300px'>" + escape(ReminderService.message(l, today)) + "</body></html>",
                    14f, Font.BOLD, Theme.TEXT);
            row.add(msg, BorderLayout.CENTER);
            row.add(Ui.muted("Due " + Ui.DATE_FMT.format(l.dueDate)), BorderLayout.SOUTH);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 4));
            body.add(row);
            body.add(Box.createVerticalStrut(8));
        }
        JScrollPane sp = Ui.scroll(body);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setPreferredSize(new Dimension(440, Math.min(300, Math.max(90, list.size() * 84))));
        Ui.dialog(owner, "Loan reminders",
                list.size() == 1 ? "1 loan needs your attention" : list.size() + " loans need your attention",
                sp, "Open Loans", "Dismiss", () -> { openLoans.run(); return null; });
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
