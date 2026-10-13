package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen {
    private final JTextField name = Ui.field("Your name");
    private final JTextField currency = Ui.field("₹");
    private final JSpinner lead = new JSpinner(new SpinnerNumberModel(3, 0, 30, 1));
    private final JCheckBox tray = check("Show system notifications for reminders");
    private final JCheckBox background = check("Keep running in the system tray when I close the window");
    private final JLabel status = Ui.label(" ", 13f, Font.PLAIN, Theme.GREEN);

    public SettingsScreen(MainFrame app, DataStore store) {
        super(app, store);
        add(header("Settings", Ui.muted("Personalise SpendWise and manage your data")), BorderLayout.NORTH);

        lead.setPreferredSize(new Dimension(90, 38));
        lead.setFont(Theme.font(Font.PLAIN, 14f));
        currency.setPreferredSize(new Dimension(90, 38));

        Page page = new Page(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(0, 0, 16, 0);

        // profile
        Ui.Form profile = new Ui.Form().row("Your name (shown in the greeting)", name).row("Currency symbol", currency);
        g.gridy = 0;
        page.add(section("Profile", profile), g);

        // reminders
        JPanel rem = new JPanel(new GridBagLayout());
        rem.setOpaque(false);
        GridBagConstraints r = new GridBagConstraints();
        r.gridx = 0; r.anchor = GridBagConstraints.WEST; r.weightx = 1; r.fill = GridBagConstraints.HORIZONTAL;
        JPanel leadRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leadRow.setOpaque(false);
        leadRow.add(Ui.label("Remind me", 14f, Font.PLAIN, Theme.TEXT));
        leadRow.add(lead);
        leadRow.add(Ui.label("days before a payback or collection date (and every day after it is overdue)", 14f, Font.PLAIN, Theme.TEXT));
        r.gridy = 0; r.insets = new Insets(0, 0, 12, 0);
        rem.add(leadRow, r);
        r.gridy = 1; r.insets = new Insets(0, 0, 6, 0);
        rem.add(tray, r);
        r.gridy = 2;
        rem.add(background, r);
        JLabel note = Ui.muted(app.reminders().trayAvailable()
                ? "Reminders are checked when the app opens and every 10 minutes while it runs."
                : "This system has no tray support, so reminders are shown as pop-up windows.");
        r.gridy = 3; r.insets = new Insets(4, 4, 12, 0);
        rem.add(note, r);
        Ui.Btn test = Ui.secondary("Send test reminder");
        test.addActionListener(e -> app.testReminder());
        JPanel tw = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tw.setOpaque(false);
        tw.add(test);
        r.gridy = 4; r.insets = new Insets(0, 0, 0, 0);
        rem.add(tw, r);
        g.gridy = 1;
        page.add(section("Loan reminders", rem), g);

        // data
        JPanel data = new JPanel(new GridBagLayout());
        data.setOpaque(false);
        GridBagConstraints d = new GridBagConstraints();
        d.gridx = 0; d.anchor = GridBagConstraints.WEST; d.weightx = 1; d.fill = GridBagConstraints.HORIZONTAL;
        d.gridy = 0; d.insets = new Insets(0, 0, 12, 0);
        data.add(Ui.muted("Your data is stored on this computer in:  " + store.directory().toAbsolutePath()), d);
        Ui.Btn export = Ui.secondary("Export all transactions (CSV)");
        export.addActionListener(e -> exportAll());
        Ui.Btn reset = Ui.danger("Delete all data");
        reset.addActionListener(e -> {
            if (Ui.confirm(this, "Delete all data",
                    "This permanently deletes every transaction, loan and goal.\nThis cannot be undone.", "Delete everything")) {
                store.resetAll();
                status.setText("All data deleted.");
            }
        });
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        btns.add(export); btns.add(reset);
        d.gridy = 1; d.insets = new Insets(0, 0, 0, 0);
        data.add(btns, d);
        g.gridy = 2;
        page.add(section("Your data", data), g);

        Ui.Btn save = Ui.primary("Save settings");
        save.addActionListener(e -> save());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        foot.setOpaque(false);
        foot.add(save); foot.add(status);
        g.gridy = 3; g.insets = new Insets(0, 0, 0, 0);
        page.add(foot, g);

        add(Ui.scroll(page), BorderLayout.CENTER);
    }

    private static JCheckBox check(String text) {
        JCheckBox c = new JCheckBox(text);
        c.setOpaque(false);
        c.setForeground(Theme.TEXT);
        c.setFont(Theme.font(Font.PLAIN, 14f));
        return c;
    }

    private JPanel section(String title, JComponent body) {
        Ui.Card c = new Ui.Card();
        c.setLayout(new BorderLayout(0, 14));
        c.add(Ui.label(title, 16f, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    private void save() {
        String cur = currency.getText().trim();
        if (cur.isEmpty()) { status.setForeground(Theme.RED); status.setText("Currency symbol can't be empty."); return; }
        store.settings.userName = name.getText().trim();
        store.settings.currency = cur;
        try { ((JSpinner.DefaultEditor) lead.getEditor()).commitEdit(); } catch (java.text.ParseException ignored) {}
        store.settings.leadDays = (Integer) lead.getValue();
        store.settings.trayNotifications = tray.isSelected();
        store.settings.runInBackground = background.isSelected();
        store.saveSettings();
        status.setForeground(Theme.GREEN);
        status.setText("Settings saved.");
    }

    private void exportAll() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Date", "Description", "Category", "Type", "Amount"});
        for (Transaction t : store.transactions)
            rows.add(new String[]{t.date.toString(), t.description, t.category, t.type, String.valueOf(t.amount)});
        exportCsv("spendwise-all-transactions.csv", rows);
    }

    @Override public void refresh() {
        name.setText(store.settings.userName);
        currency.setText(store.settings.currency);
        lead.setValue(store.settings.leadDays);
        tray.setSelected(store.settings.trayNotifications);
        background.setSelected(store.settings.runInBackground);
        background.setEnabled(app.reminders().trayAvailable());
        tray.setEnabled(app.reminders().trayAvailable());
    }
}
