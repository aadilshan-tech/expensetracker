package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class DashboardScreen extends Screen {
    private final JLabel greeting = Ui.title(" ");
    private final JLabel dateLabel = Ui.muted(" ");
    private final StatCard balance = new StatCard("Total balance", Theme.GREEN);
    private final StatCard income = new StatCard("Income · this month", Theme.BLUE);
    private final StatCard expense = new StatCard("Expenses · this month", Theme.RED);
    private final StatCard savings = new StatCard("Savings rate · this month", Theme.VIOLET);
    private final Charts.CashFlowChart cashFlow = new Charts.CashFlowChart();
    private final Charts.DonutChart donut = new Charts.DonutChart();
    private final JLabel donutTitle = Ui.label("Spending by category", 16f, Font.BOLD, Theme.TEXT);
    private final JPanel banner = new JPanel(new BorderLayout());
    private final JPanel recentList = new JPanel();
    private final JPanel loanBox = new JPanel();

    public DashboardScreen(MainFrame app, DataStore store) {
        super(app, store);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        Ui.Btn add = Ui.primary("+  Add transaction");
        add.addActionListener(e -> Dialogs.transaction(app, store, null));
        Ui.Btn borrow = Ui.secondary("Borrow");
        borrow.addActionListener(e -> Dialogs.loan(app, store, Loan.Direction.BORROWED));
        Ui.Btn lend = Ui.secondary("Lend");
        lend.addActionListener(e -> Dialogs.loan(app, store, Loan.Direction.LENT));

        Page page = new Page(new GridBagLayout());
        page.setBorder(new EmptyBorder(28, 32, 24, 32));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.BOTH;

        g.gridy = 0;
        page.add(header(greeting, dateLabel, borrow, lend, add), g);

        banner.setOpaque(false);
        g.gridy = 1; g.insets = new Insets(0, 0, 18, 0);
        page.add(banner, g);

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(balance); stats.add(income); stats.add(expense); stats.add(savings);
        g.gridy = 2; g.insets = new Insets(0, 0, 18, 0);
        page.add(stats, g);

        // charts row
        JPanel charts = new JPanel(new GridBagLayout());
        charts.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        Ui.Card flowCard = card("Cash flow", "Last 6 months", cashFlow);
        Ui.Card donutCard = new Ui.Card();
        donutCard.setLayout(new BorderLayout(0, 8));
        donutCard.add(donutTitle, BorderLayout.NORTH);
        donutCard.add(donut, BorderLayout.CENTER);
        c.gridx = 0; c.weightx = 0.58; c.insets = new Insets(0, 0, 0, 16);
        charts.add(flowCard, c);
        c.gridx = 1; c.weightx = 0.42; c.insets = new Insets(0, 0, 0, 0);
        charts.add(donutCard, c);
        g.gridy = 3; g.weighty = 0.5; g.insets = new Insets(0, 0, 18, 0);
        page.add(charts, g);

        // bottom row
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        recentList.setOpaque(false);
        recentList.setLayout(new BoxLayout(recentList, BoxLayout.Y_AXIS));
        Ui.Card recentCard = new Ui.Card();
        recentCard.setLayout(new BorderLayout(0, 8));
        JPanel rh = new JPanel(new BorderLayout());
        rh.setOpaque(false);
        rh.add(Ui.label("Recent transactions", 16f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        Ui.Btn all = Ui.ghost("View all");
        all.addActionListener(e -> app.show("Transactions"));
        rh.add(all, BorderLayout.EAST);
        recentCard.add(rh, BorderLayout.NORTH);
        recentCard.add(recentList, BorderLayout.CENTER);

        loanBox.setOpaque(false);
        loanBox.setLayout(new BoxLayout(loanBox, BoxLayout.Y_AXIS));
        Ui.Card loanCard = new Ui.Card();
        loanCard.setLayout(new BorderLayout(0, 8));
        JPanel lh = new JPanel(new BorderLayout());
        lh.setOpaque(false);
        lh.add(Ui.label("Borrowed & lent", 16f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        Ui.Btn manage = Ui.ghost("Manage");
        manage.addActionListener(e -> app.show("Loans"));
        lh.add(manage, BorderLayout.EAST);
        loanCard.add(lh, BorderLayout.NORTH);
        loanCard.add(loanBox, BorderLayout.CENTER);

        c.gridx = 0; c.weightx = 0.58; c.insets = new Insets(0, 0, 0, 16);
        bottom.add(recentCard, c);
        c.gridx = 1; c.weightx = 0.42; c.insets = new Insets(0, 0, 0, 0);
        bottom.add(loanCard, c);
        g.gridy = 4; g.weighty = 0.5; g.insets = new Insets(0, 0, 0, 0);
        page.add(bottom, g);

        JScrollPane sp = Ui.scroll(page);
        add(sp, BorderLayout.CENTER);
    }

    private JPanel header(JLabel title, JLabel sub, JComponent... actions) {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 20, 0));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 4));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        for (JComponent c : actions) right.add(c);
        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private Ui.Card card(String title, String sub, JComponent body) {
        Ui.Card c = new Ui.Card();
        c.setLayout(new BorderLayout(0, 8));
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.add(Ui.label(title, 16f, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        h.add(Ui.muted(sub), BorderLayout.EAST);
        c.add(h, BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    // ------------------------------------------------------------------ refresh

    @Override public void refresh() {
        LocalDate today = LocalDate.now();
        int hour = java.time.LocalTime.now().getHour();
        String part = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        String name = store.settings.userName.isBlank() ? "" : ", " + store.settings.userName;
        greeting.setText(part + name);
        dateLabel.setText(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));

        YearMonth now = YearMonth.from(today), prev = now.minusMonths(1);
        double inc = store.income(now), exp = store.expense(now);
        balance.set(Theme.money(store.balance()),
                store.totalOwed() > 0 || store.totalReceivable() > 0 ? "Includes loan money" : "All time", Theme.MUTED);
        income.set(Theme.money(inc), delta(inc, store.income(prev), true), deltaColor(inc, store.income(prev), true));
        expense.set(Theme.money(exp), delta(exp, store.expense(prev), false), deltaColor(exp, store.expense(prev), false));
        double rate = inc > 0 ? (inc - exp) / inc * 100 : 0;
        savings.set(inc > 0 ? String.format(Locale.ROOT, "%.1f%%", rate) : "–",
                inc > 0 ? (inc - exp >= 0 ? "Saved " : "Overspent ") + Theme.money(Math.abs(inc - exp)) : "No income this month",
                inc > 0 && rate < 0 ? Theme.RED : Theme.MUTED);

        // cash flow, last 6 months
        String[] labels = new String[6];
        double[] in = new double[6], out = new double[6];
        for (int i = 0; i < 6; i++) {
            YearMonth m = now.minusMonths(5 - i);
            labels[i] = m.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
            in[i] = store.income(m);
            out[i] = store.expense(m);
        }
        cashFlow.setData(labels, in, out);

        // spending donut: this month, or all time when nothing was spent yet this month
        LinkedHashMap<String, Double> cats = store.expenseByCategory(today.withDayOfMonth(1), today);
        if (cats.isEmpty()) {
            cats = store.expenseByCategory(null, null);
            donutTitle.setText("Spending by category · all time");
            donut.setData(cats, "All time");
        } else {
            donutTitle.setText("Spending by category · this month");
            donut.setData(cats, "This month");
        }

        buildBanner(today);
        buildRecent();
        buildLoanBox(today);
        revalidate();
        repaint();
    }

    private static String delta(double cur, double prev, boolean upIsGood) {
        if (prev <= 0) return cur > 0 ? "First entries of the month" : "No data last month";
        double pct = (cur - prev) / prev * 100;
        return String.format(Locale.ROOT, "%+.0f%% vs last month", pct);
    }

    private static Color deltaColor(double cur, double prev, boolean upIsGood) {
        if (prev <= 0 || cur == prev) return Theme.MUTED;
        boolean up = cur > prev;
        return up == upIsGood ? Theme.GREEN : Theme.RED;
    }

    private void buildBanner(LocalDate today) {
        banner.removeAll();
        List<Loan> due = store.pendingReminders(today);
        if (due.isEmpty()) { banner.setVisible(false); return; }
        banner.setVisible(true);
        boolean overdue = false;
        for (Loan l : due) if (l.status(today, store.settings.leadDays) == Loan.Status.OVERDUE) overdue = true;
        Color accent = overdue ? Theme.RED : Theme.AMBER;

        Ui.Card card = new Ui.Card(16, Theme.alpha(accent, 22)).accent(accent);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(14, 22, 14, 18));
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 3));
        text.setOpaque(false);
        text.add(Ui.label(due.size() == 1 ? "1 loan reminder" : due.size() + " loan reminders", 14.5f, Font.BOLD, accent));
        int shown = 0;
        for (Loan l : due) {
            if (shown++ == 3) { text.add(Ui.muted("+ " + (due.size() - 3) + " more"));  break; }
            text.add(Ui.label(ReminderService.message(l, today), 13.5f, Font.PLAIN, Theme.TEXT));
        }
        Ui.Btn view = Ui.secondary("Open loans");
        view.addActionListener(e -> app.show("Loans"));
        JPanel viewWrap = new JPanel(new GridBagLayout());
        viewWrap.setOpaque(false);
        viewWrap.add(view);
        card.add(text, BorderLayout.CENTER);
        card.add(viewWrap, BorderLayout.EAST);
        banner.add(card, BorderLayout.CENTER);
    }

    private void buildRecent() {
        recentList.removeAll();
        List<Transaction> sorted = new ArrayList<>(store.transactions);
        Collections.reverse(sorted);
        sorted.sort((a, b) -> b.date.compareTo(a.date));
        if (sorted.isEmpty()) {
            recentList.add(empty("No transactions yet", "Add your first income or expense."));
        }
        int n = 0;
        for (Transaction t : sorted) {
            if (n++ == 6) break;
            recentList.add(txRow(t));
        }
        recentList.add(Box.createVerticalGlue());
    }

    private JComponent txRow(Transaction t) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(7, 0, 7, 0));
        Color c = Theme.categoryColor(t.category);
        row.add(new Avatar(t.category.isEmpty() ? "?" : t.category.substring(0, 1).toUpperCase(), c), BorderLayout.WEST);
        JPanel mid = new JPanel(new GridLayout(2, 1, 0, 1));
        mid.setOpaque(false);
        mid.add(Ui.label(t.description, 14f, Font.BOLD, Theme.TEXT));
        mid.add(Ui.muted(t.category + "  ·  " + Ui.DATE_FMT.format(t.date)));
        row.add(mid, BorderLayout.CENTER);
        JLabel amt = Ui.label((t.isIncome() ? "+" : "-") + Theme.money(t.amount), 14f, Font.BOLD, t.isIncome() ? Theme.GREEN : Theme.RED);
        row.add(amt, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void buildLoanBox(LocalDate today) {
        loanBox.removeAll();
        List<Loan> open = new ArrayList<>();
        for (Loan l : store.loans) if (!l.isSettled()) open.add(l);
        open.sort(Comparator.comparing(l -> l.dueDate));

        if (store.loans.isEmpty()) {
            loanBox.add(empty("No borrowed or lent money", "Track who owes you and what you owe, with payback reminders."));
            loanBox.add(Box.createVerticalGlue());
            return;
        }
        JPanel totals = new JPanel(new GridLayout(1, 2, 12, 0));
        totals.setOpaque(false);
        totals.add(miniStat("You owe", store.totalOwed(), Theme.RED));
        totals.add(miniStat("You're owed", store.totalReceivable(), Theme.GREEN));
        totals.setAlignmentX(Component.LEFT_ALIGNMENT);
        totals.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        loanBox.add(totals);
        loanBox.add(Box.createVerticalStrut(10));

        if (open.isEmpty()) {
            loanBox.add(empty("All settled", "Nothing is pending right now."));
        }
        int n = 0;
        for (Loan l : open) {
            if (n++ == 3) break;
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(6, 0, 6, 0));
            JPanel mid = new JPanel(new GridLayout(2, 1, 0, 1));
            mid.setOpaque(false);
            mid.add(Ui.label(l.person, 14f, Font.BOLD, Theme.TEXT));
            mid.add(Ui.muted((l.isBorrowed() ? "Pay back " : "Collect ") + Theme.money(l.remaining())));
            Loan.Status s = l.status(today, store.settings.leadDays);
            Color pc = s == Loan.Status.OVERDUE ? Theme.RED : (s == Loan.Status.ACTIVE ? Theme.MUTED : Theme.AMBER);
            JPanel pillWrap = new JPanel(new GridBagLayout());
            pillWrap.setOpaque(false);
            pillWrap.add(new Ui.Pill(Loan.statusText(l, today, store.settings.leadDays), pc));
            row.add(mid, BorderLayout.CENTER);
            row.add(pillWrap, BorderLayout.EAST);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            loanBox.add(row);
        }
        loanBox.add(Box.createVerticalGlue());
    }

    private JComponent miniStat(String label, double value, Color c) {
        Ui.Card card = new Ui.Card(14, Theme.CARD_ALT);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setLayout(new GridLayout(2, 1, 0, 1));
        card.add(Ui.muted(label));
        card.add(Ui.label(Theme.money(value), 18f, Font.BOLD, c));
        return card;
    }

    private JComponent empty(String title, String sub) {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 4));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 0, 24, 0));
        JLabel a = Ui.label(title, 14f, Font.BOLD, Theme.TEXT);
        JLabel b = Ui.muted("<html><body style='width:260px'>" + sub + "</body></html>");
        a.setHorizontalAlignment(SwingConstants.CENTER);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(a); p.add(b);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** Colored circle with a letter. */
    static class Avatar extends JComponent {
        private final String letter;
        private final Color color;
        Avatar(String letter, Color color) {
            this.letter = letter; this.color = color;
            setPreferredSize(new Dimension(38, 38));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Theme.alpha(color, 40));
            g.fill(new Ellipse2D.Double(0, (getHeight() - 38) / 2.0, 38, 38));
            g.setColor(color);
            g.setFont(Theme.font(Font.BOLD, 15f));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(letter, (38 - fm.stringWidth(letter)) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
