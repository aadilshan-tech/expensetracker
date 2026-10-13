package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.*;

/** The application window: sidebar navigation plus the page area. */
public class MainFrame extends JFrame {
    private static final String[][] NAV = {
            {"Dashboard", "Dashboard"}, {"Add", "Add transaction"}, {"Transactions", "Transactions"},
            {"Loans", "Borrowed & Lent"}, {"Categories", "Categories"}, {"Reports", "Reports"},
            {"Goals", "Goals"}, {"Settings", "Settings"}
    };

    private final DataStore store;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final ReminderService reminders;
    private String current = "Dashboard";

    public MainFrame(DataStore store) {
        this.store = store;
        reminders = new ReminderService(store,
                () -> Dialogs.reminders(this, store, () -> show("Loans")),
                this::showFront);

        setTitle("SpendWise");
        setIconImage(appIcon());
        setSize(1360, 880);
        setMinimumSize(new Dimension(1120, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(createSidebar(), BorderLayout.WEST);

        content.setOpaque(false);
        addScreen("Dashboard", new DashboardScreen(this, store));
        addScreen("Transactions", new TransactionsScreen(this, store));
        addScreen("Loans", new LoansScreen(this, store));
        addScreen("Categories", new CategoriesScreen(this, store));
        addScreen("Reports", new ReportsScreen(this, store));
        addScreen("Goals", new GoalsScreen(this, store));
        addScreen("Settings", new SettingsScreen(this, store));
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        store.addListener(this::refreshAll);
        store.addListener(() -> reminders.check(true)); // new / changed loans are not announced again
        refreshAll();
        show("Dashboard");

        getRootPane().registerKeyboardAction(e -> Dialogs.transaction(this, store, null),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (store.settings.runInBackground && reminders.ensureTray()) setVisible(false);
                else quit();
            }
        });
    }

    private void addScreen(String key, Screen s) {
        screens.put(key, s);
        content.add(s, key);
    }

    public ReminderService reminders() { return reminders; }

    /** Call once the window is visible: starts reminders and shows what is due right now. */
    public void startReminders() {
        reminders.start();
        if (!store.pendingReminders(LocalDate.now()).isEmpty())
            Dialogs.reminders(this, store, () -> show("Loans"));
    }

    public void testReminder() { reminders.sendTest(); }

    private void quit() {
        reminders.stop();
        dispose();
        System.exit(0);
    }

    private void showFront() {
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
        requestFocus();
        if (!store.pendingReminders(LocalDate.now()).isEmpty()) show("Loans");
    }

    public void show(String key) {
        if (!screens.containsKey(key)) return;
        current = key;
        cards.show(content, key);
        for (NavButton b : navButtons.values()) b.repaint();
    }

    private void refreshAll() {
        for (Screen s : screens.values()) s.refresh();
        NavButton loans = navButtons.get("Loans");
        if (loans != null) loans.badge = store.pendingReminders(LocalDate.now()).size();
        for (NavButton b : navButtons.values()) b.repaint();
    }

    // ------------------------------------------------------------------ sidebar

    private JPanel createSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.SIDEBAR);
        side.setPreferredSize(new Dimension(250, 0));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        side.add(new Logo(), BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(new EmptyBorder(8, 14, 8, 14));
        for (String[] item : NAV) {
            NavButton b = new NavButton(item[0], item[1]);
            navButtons.put(item[0], b);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            b.addActionListener(e -> {
                if (item[0].equals("Add")) Dialogs.transaction(this, store, null);
                else show(item[0]);
            });
            menu.add(b);
            menu.add(Box.createVerticalStrut(4));
        }
        side.add(menu, BorderLayout.CENTER);

        JLabel foot = Ui.label("SpendWise 2.0   ·   Ctrl+N to add", 11.5f, Font.PLAIN, Theme.MUTED);
        foot.setBorder(new EmptyBorder(0, 28, 20, 0));
        side.add(foot, BorderLayout.SOUTH);
        return side;
    }

    private static Image appIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.GREEN);
        g.fill(new RoundRectangle2D.Double(0, 0, 64, 64, 18, 18));
        g.setColor(new Color(0x04130D));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("S", (64 - fm.stringWidth("S")) / 2, 48);
        g.dispose();
        return img;
    }

    private static class Logo extends JComponent {
        Logo() { setPreferredSize(new Dimension(250, 96)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.drawImage(appIcon(), 28, 30, 40, 40, null);
            g.setColor(Theme.TEXT);
            g.setFont(Theme.font(Font.BOLD, 20f));
            g.drawString("SpendWise", 80, 48);
            g.setColor(Theme.MUTED);
            g.setFont(Theme.font(Font.PLAIN, 11.5f));
            g.drawString("Personal finance", 80, 65);
            g.dispose();
        }
    }

    private class NavButton extends JButton {
        final String key;
        int badge;
        private boolean hover;

        NavButton(String key, String label) {
            super(label);
            this.key = key;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(220, 46));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean sel = key.equals(current);
            int w = getWidth(), h = getHeight();
            if (sel) {
                g.setColor(Theme.alpha(Theme.GREEN, 30));
                g.fill(new RoundRectangle2D.Double(0, 0, w, h, 14, 14));
                g.setColor(Theme.GREEN);
                g.fill(new RoundRectangle2D.Double(0, 11, 4, h - 22, 4, 4));
            } else if (hover) {
                g.setColor(Theme.alpha(Color.WHITE, 12));
                g.fill(new RoundRectangle2D.Double(0, 0, w, h, 14, 14));
            }
            Color fg = sel ? Theme.GREEN : hover ? Theme.TEXT : Theme.MUTED;
            Icons.nav(key, fg).paintIcon(this, g, 18, (h - 20) / 2);
            g.setColor(sel ? Theme.TEXT : fg);
            g.setFont(Theme.font(sel ? Font.BOLD : Font.PLAIN, 14.5f));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), 52, (h - fm.getHeight()) / 2 + fm.getAscent());
            if (badge > 0) {
                String t = String.valueOf(badge);
                g.setFont(Theme.font(Font.BOLD, 11.5f));
                fm = g.getFontMetrics();
                int bw = Math.max(22, fm.stringWidth(t) + 12);
                int bx = w - bw - 12, by = (h - 22) / 2;
                g.setColor(Theme.RED);
                g.fill(new RoundRectangle2D.Double(bx, by, bw, 22, 22, 22));
                g.setColor(Color.WHITE);
                g.drawString(t, bx + (bw - fm.stringWidth(t)) / 2, by + (22 - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }
}
