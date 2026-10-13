package spendwise;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

/** User preferences, stored as a small properties file. */
public class Settings {
    public String userName = "Aadil";
    public String currency = "₹";
    /** Start reminding this many days before a loan is due. */
    public int leadDays = 3;
    public boolean trayNotifications = true;
    public boolean runInBackground = false;

    public void load(Path file) {
        if (!file.toFile().exists()) return;
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) { e.printStackTrace(); return; }
        userName = p.getProperty("userName", userName);
        currency = p.getProperty("currency", currency);
        trayNotifications = Boolean.parseBoolean(p.getProperty("trayNotifications", "true"));
        runInBackground = Boolean.parseBoolean(p.getProperty("runInBackground", "false"));
        try { leadDays = Integer.parseInt(p.getProperty("leadDays", "3")); } catch (NumberFormatException ignored) {}
    }

    public void save(Path file) {
        Properties p = new Properties();
        p.setProperty("userName", userName);
        p.setProperty("currency", currency);
        p.setProperty("leadDays", String.valueOf(leadDays));
        p.setProperty("trayNotifications", String.valueOf(trayNotifications));
        p.setProperty("runInBackground", String.valueOf(runInBackground));
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8)) {
            p.store(w, "SpendWise settings");
        } catch (IOException e) { e.printStackTrace(); }
    }
}
