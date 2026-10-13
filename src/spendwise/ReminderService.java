package spendwise;

import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Watches loan due dates. Reminders appear when the app starts (handled by the main window), and then
 * every few minutes while the app is running (system tray notification, or a dialog when no tray exists).
 */
public class ReminderService {
    private final DataStore store;
    private final Runnable showDialog;   // fallback UI when no tray is available
    private final Runnable openApp;      // bring the window to the front / open Loans
    private final Set<String> notified = new HashSet<>();
    private LocalDate notifiedDay = LocalDate.now();
    private Timer timer;
    private TrayIcon tray;

    public ReminderService(DataStore store, Runnable showDialog, Runnable openApp) {
        this.store = store;
        this.showDialog = showDialog;
        this.openApp = openApp;
    }

    public static String message(Loan l, LocalDate today) {
        long d = l.daysUntilDue(today);
        String amt = Theme.money(l.remaining());
        String late = (-d) + (-d == 1 ? " day late" : " days late");
        String when = d == 1 ? "tomorrow" : "in " + d + " days";
        if (l.isBorrowed()) {
            if (d < 0) return "Overdue: pay back " + amt + " to " + l.person + " (" + late + ")";
            if (d == 0) return "Pay back " + amt + " to " + l.person + " today";
            return "Pay back " + amt + " to " + l.person + " " + when;
        }
        if (d < 0) return "Overdue: " + l.person + " owes you " + amt + " (" + late + ")";
        if (d == 0) return "Collect " + amt + " from " + l.person + " today";
        return "Collect " + amt + " from " + l.person + " " + when;
    }

    /** Starts the periodic check. Everything already due is considered "seen" (the window shows it at launch). */
    public void start() {
        check(true);
        timer = new Timer(10 * 60 * 1000, e -> check(false));
        timer.setInitialDelay(60 * 1000);
        timer.start();
    }

    public void stop() {
        if (timer != null) timer.stop();
        if (tray != null) { SystemTray.getSystemTray().remove(tray); tray = null; }
    }

    /**
     * @param silent when true, new reminders are only marked as seen and nothing is shown
     */
    public void check(boolean silent) {
        LocalDate today = LocalDate.now();
        if (!today.equals(notifiedDay)) { notified.clear(); notifiedDay = today; }
        List<Loan> fresh = new ArrayList<>();
        for (Loan l : store.pendingReminders(today)) {
            if (notified.add(l.id + ":" + l.status(today, store.settings.leadDays))) fresh.add(l);
        }
        if (silent || fresh.isEmpty()) return;
        if (store.settings.trayNotifications && ensureTray()) {
            int shown = 0;
            for (Loan l : fresh) {
                if (shown++ == 3) { tray.displayMessage("SpendWise", "…and " + (fresh.size() - 3) + " more reminders",
                        TrayIcon.MessageType.WARNING); break; }
                tray.displayMessage(l.isBorrowed() ? "Payback reminder" : "Money to collect",
                        message(l, today), TrayIcon.MessageType.WARNING);
            }
        } else {
            EventQueue.invokeLater(showDialog);
        }
    }

    public boolean trayAvailable() { return SystemTray.isSupported(); }

    /** Lets the user check that notifications work on their system. */
    public void sendTest() {
        if (store.settings.trayNotifications && ensureTray()) {
            tray.displayMessage("SpendWise", "Reminders are working. You'll be notified here when a payback is due.",
                    TrayIcon.MessageType.INFO);
        } else {
            EventQueue.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(null,
                    "Reminders are working. A window like this appears when a payback is due.",
                    "SpendWise", javax.swing.JOptionPane.INFORMATION_MESSAGE));
        }
    }

    /** Creates the tray icon on first use. Returns false when the platform has no system tray. */
    public boolean ensureTray() {
        if (tray != null) return true;
        if (!SystemTray.isSupported()) return false;
        try {
            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem("Open SpendWise");
            open.addActionListener(e -> EventQueue.invokeLater(openApp));
            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(e -> System.exit(0));
            menu.add(open);
            menu.addSeparator();
            menu.add(exit);
            tray = new TrayIcon(icon(), "SpendWise", menu);
            tray.setImageAutoSize(true);
            tray.addActionListener(e -> EventQueue.invokeLater(openApp));
            SystemTray.getSystemTray().add(tray);
            return true;
        } catch (Exception ex) {
            tray = null;
            return false;
        }
    }

    private static Image icon() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.GREEN);
        g.fillRoundRect(0, 0, 32, 32, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("S", (32 - fm.stringWidth("S")) / 2, 24);
        g.dispose();
        return img;
    }
}
