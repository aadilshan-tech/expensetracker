package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class GoalsScreen extends Screen {
    private final JLabel subtitle = Ui.muted(" ");
    private final JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
    private final JPanel emptyBox = new JPanel(new GridBagLayout());
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);

    public GoalsScreen(MainFrame app, DataStore store) {
        super(app, store);
        Ui.Btn add = Ui.primary("+  New goal");
        add.addActionListener(e -> Dialogs.goal(app, store));
        add(header("Savings goals", subtitle, add), BorderLayout.NORTH);

        grid.setOpaque(false);
        Page page = new Page(new BorderLayout());
        page.add(grid, BorderLayout.NORTH);
        body.setOpaque(false);
        body.add(Ui.scroll(page), "goals");

        emptyBox.setOpaque(false);
        JPanel box = new JPanel(new GridLayout(0, 1, 0, 12));
        box.setOpaque(false);
        JLabel a = Ui.label("No goals yet", 18f, Font.BOLD, Theme.TEXT);
        JLabel b = Ui.muted("Create a goal like \"New laptop\" or \"Emergency fund\" and track your progress.");
        a.setHorizontalAlignment(SwingConstants.CENTER);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        Ui.Btn create = Ui.primary("Create your first goal");
        create.addActionListener(e -> Dialogs.goal(app, store));
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnWrap.setOpaque(false);
        btnWrap.add(create);
        box.add(a); box.add(b); box.add(btnWrap);
        emptyBox.add(box);
        body.add(emptyBox, "empty");
        add(body, BorderLayout.CENTER);
    }

    @Override public void refresh() {
        grid.removeAll();
        double saved = 0, target = 0;
        int done = 0;
        for (Goal g : store.goals) {
            grid.add(goalCard(g));
            saved += g.saved; target += g.target;
            if (g.reached()) done++;
        }
        // keep a single goal from stretching over the full row
        if (store.goals.size() % 2 == 1) { JPanel filler = new JPanel(); filler.setOpaque(false); grid.add(filler); }
        subtitle.setText(store.goals.isEmpty() ? "Save towards the things that matter"
                : Theme.money(saved) + " saved of " + Theme.money(target) + "  ·  " + done + " of " + store.goals.size() + " reached");
        cards.show(body, store.goals.isEmpty() ? "empty" : "goals");
        revalidate();
        repaint();
    }

    private JComponent goalCard(Goal g) {
        LocalDate today = LocalDate.now();
        Color accent = g.reached() ? Theme.GREEN : Theme.BLUE;
        Ui.Card c = new Ui.Card().accent(accent);
        c.setLayout(new BorderLayout(0, 12));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(Ui.label(g.name, 17f, Font.BOLD, Theme.TEXT), BorderLayout.CENTER);
        top.add(new Ui.Pill(g.reached() ? "Reached" : Math.round(g.progress() * 100) + "%", accent), BorderLayout.EAST);

        JPanel mid = new JPanel(new GridLayout(0, 1, 0, 8));
        mid.setOpaque(false);
        mid.add(Ui.label(Theme.money(g.saved) + "  of  " + Theme.money(g.target), 22f, Font.BOLD, Theme.TEXT));
        Charts.ProgressBar bar = new Charts.ProgressBar(accent);
        bar.set(g.progress(), accent);
        bar.setPreferredSize(new Dimension(100, 12));
        mid.add(bar);
        mid.add(Ui.muted(info(g, today)));

        Ui.Btn add = Ui.primary("Add funds");
        add.addActionListener(e -> Dialogs.goalFunds(app, store, g, true));
        Ui.Btn take = Ui.secondary("Withdraw");
        take.setEnabled(g.saved > 0);
        take.addActionListener(e -> Dialogs.goalFunds(app, store, g, false));
        Ui.Btn del = Ui.danger("Delete");
        del.addActionListener(e -> {
            if (Ui.confirm(this, "Delete goal", "Delete the goal \"" + g.name + "\"?", "Delete")) {
                store.goals.remove(g);
                store.changed();
            }
        });
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.add(add); btns.add(take); btns.add(del);

        c.add(top, BorderLayout.NORTH);
        c.add(mid, BorderLayout.CENTER);
        c.add(btns, BorderLayout.SOUTH);
        return c;
    }

    private static String info(Goal g, LocalDate today) {
        if (g.reached()) return "Goal reached. Well done!";
        double left = g.target - g.saved;
        String s = Theme.money(left) + " to go";
        if (g.targetDate != null) {
            long days = ChronoUnit.DAYS.between(today, g.targetDate);
            if (days < 0) return s + "  ·  deadline passed " + (-days) + (-days == 1 ? " day ago" : " days ago");
            long months = Math.max(1, (long) Math.ceil(days / 30.0));
            s += "  ·  " + days + (days == 1 ? " day left" : " days left")
                    + "  ·  save " + Theme.money(Math.ceil(left / months)) + "/month";
        }
        return s;
    }
}
