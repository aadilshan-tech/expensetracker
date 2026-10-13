package spendwise;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Holds all app data, saves it to disk after every change and notifies listeners.
 * Data lives in ~/.spendwise (override with -Dspendwise.dir=...).
 */
public class DataStore {
    public final List<Transaction> transactions = new ArrayList<>();
    public final List<Loan> loans = new ArrayList<>();
    public final List<Goal> goals = new ArrayList<>();
    public final Settings settings = new Settings();

    private final Path dir;
    private final Path txFile, loanFile, goalFile, settingsFile;
    private final List<Runnable> listeners = new ArrayList<>();

    public DataStore() {
        this(Paths.get(System.getProperty("spendwise.dir",
                System.getProperty("user.home") + java.io.File.separator + ".spendwise")));
    }

    public DataStore(Path dir) {
        this.dir = dir;
        this.txFile = dir.resolve("transactions.csv");
        this.loanFile = dir.resolve("loans.csv");
        this.goalFile = dir.resolve("goals.csv");
        this.settingsFile = dir.resolve("settings.properties");
        try { Files.createDirectories(dir); } catch (IOException e) { e.printStackTrace(); }
        load();
    }

    public Path directory() { return dir; }

    // ------------------------------------------------------------------ listeners

    public void addListener(Runnable r) { listeners.add(r); }

    /** Persist everything and tell the UI to refresh. Call after any mutation. */
    public void changed() {
        save();
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }

    // ------------------------------------------------------------------ file codec
    // Fields are joined with '|'. '|', '\' and newlines are escaped, so any text is safe.
    // Old 5-column files (date|category|description|amount|type) still load fine.

    static String encode(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append('|');
            String f = fields[i] == null ? "" : fields[i];
            for (char c : f.toCharArray()) {
                if (c == '\\') sb.append("\\\\");
                else if (c == '|') sb.append("\\|");
                else if (c == '\n') sb.append("\\n");
                else if (c == '\r') { /* drop */ }
                else sb.append(c);
            }
        }
        return sb.toString();
    }

    static List<String> decode(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                char n = line.charAt(++i);
                cur.append(n == 'n' ? '\n' : n);
            } else if (c == '|') {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }

    private static LocalDate date(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s.trim());
    }

    private static List<String> readLines(Path p) {
        try { return Files.readAllLines(p, StandardCharsets.UTF_8); }
        catch (IOException e) { return Collections.emptyList(); }
    }

    private static void writeLines(Path p, List<String> lines) {
        try {
            Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            try { Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ------------------------------------------------------------------ load / save

    private void load() {
        settings.load(settingsFile);
        Theme.currency = settings.currency;

        transactions.clear();
        Path source = txFile;
        if (!Files.exists(txFile)) {
            // first run of the new version: import the data file of the old app if it is next to us
            Path legacy = Paths.get("spendwise_data.csv");
            if (Files.exists(legacy)) source = legacy;
        }
        for (String line : readLines(source)) {
            if (line.isBlank()) continue;
            try {
                List<String> f = decode(line);
                if (f.size() >= 7) {
                    transactions.add(new Transaction(f.get(0), date(f.get(1)), f.get(2), f.get(3),
                            Double.parseDouble(f.get(4)), f.get(5), f.get(6)));
                } else if (f.size() == 5) { // legacy
                    transactions.add(new Transaction(date(f.get(0)), f.get(1).trim(), f.get(2).trim(),
                            Double.parseDouble(f.get(3)), f.get(4).trim()));
                }
            } catch (Exception ex) { System.err.println("Skipping bad transaction line: " + line); }
        }

        loans.clear();
        for (String line : readLines(loanFile)) {
            if (line.isBlank()) continue;
            try {
                List<String> f = decode(line);
                loans.add(new Loan(f.get(0), f.get(1), Loan.Direction.valueOf(f.get(2)),
                        Double.parseDouble(f.get(3)), Double.parseDouble(f.get(4)),
                        date(f.get(5)), date(f.get(6)), f.get(7), Boolean.parseBoolean(f.get(8)), date(f.get(9))));
            } catch (Exception ex) { System.err.println("Skipping bad loan line: " + line); }
        }

        goals.clear();
        for (String line : readLines(goalFile)) {
            if (line.isBlank()) continue;
            try {
                List<String> f = decode(line);
                goals.add(new Goal(f.get(0), f.get(1), Double.parseDouble(f.get(2)),
                        Double.parseDouble(f.get(3)), date(f.get(4))));
            } catch (Exception ex) { System.err.println("Skipping bad goal line: " + line); }
        }
        // if we imported legacy data, persist it in the new location right away
        if (!Files.exists(txFile) && !transactions.isEmpty()) save();
    }

    public void save() {
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        List<String> t = new ArrayList<>();
        for (Transaction x : transactions)
            t.add(encode(x.id, String.valueOf(x.date), x.category, x.description,
                    String.valueOf(x.amount), x.type, x.loanId));
        writeLines(txFile, t);

        List<String> l = new ArrayList<>();
        for (Loan x : loans)
            l.add(encode(x.id, x.person, x.direction.name(), String.valueOf(x.amount), String.valueOf(x.paid),
                    String.valueOf(x.startDate), String.valueOf(x.dueDate), x.note,
                    String.valueOf(x.linked), x.settledDate == null ? "" : x.settledDate.toString()));
        writeLines(loanFile, l);

        List<String> g = new ArrayList<>();
        for (Goal x : goals)
            g.add(encode(x.id, x.name, String.valueOf(x.target), String.valueOf(x.saved),
                    x.targetDate == null ? "" : x.targetDate.toString()));
        writeLines(goalFile, g);

        settings.save(settingsFile);
    }

    public void saveSettings() {
        Theme.currency = settings.currency;
        changed();
    }

    // ------------------------------------------------------------------ transactions

    public void addTransaction(Transaction t) { transactions.add(t); changed(); }

    public void deleteTransaction(Transaction t) { transactions.remove(t); changed(); }

    // ------------------------------------------------------------------ loans

    /** Creates a loan. When linked, the money movement is also recorded in the balance. */
    public void addLoan(Loan l) {
        loans.add(l);
        if (l.linked) {
            transactions.add(new Transaction(UUID.randomUUID().toString().substring(0, 8), l.startDate,
                    Transaction.LOAN_CATEGORY,
                    l.isBorrowed() ? "Borrowed from " + l.person : "Lent to " + l.person,
                    l.amount, l.isBorrowed() ? Transaction.INCOME : Transaction.EXPENSE, l.id));
        }
        changed();
    }

    /** Records a (possibly partial) repayment. Returns true when the loan is now fully settled. */
    public boolean recordPayment(Loan l, double amount, LocalDate date) {
        amount = Math.min(amount, l.remaining());
        if (amount <= 0) return l.isSettled();
        l.paid += amount;
        if (l.isSettled()) { l.paid = l.amount; l.settledDate = date; }
        if (l.linked) {
            transactions.add(new Transaction(UUID.randomUUID().toString().substring(0, 8), date,
                    Transaction.LOAN_CATEGORY,
                    l.isBorrowed() ? "Paid back " + l.person : "Received from " + l.person,
                    amount, l.isBorrowed() ? Transaction.EXPENSE : Transaction.INCOME, l.id));
        }
        changed();
        return l.isSettled();
    }

    public void deleteLoan(Loan l) {
        loans.remove(l);
        transactions.removeIf(t -> l.id.equals(t.loanId));
        changed();
    }

    public Loan findLoan(String id) {
        for (Loan l : loans) if (l.id.equals(id)) return l;
        return null;
    }

    /** Unsettled loans that are overdue or due within the reminder window, most urgent first. */
    public List<Loan> pendingReminders(LocalDate today) {
        List<Loan> out = new ArrayList<>();
        for (Loan l : loans) {
            Loan.Status s = l.status(today, settings.leadDays);
            if (s == Loan.Status.OVERDUE || s == Loan.Status.DUE_TODAY || s == Loan.Status.DUE_SOON) out.add(l);
        }
        out.sort(Comparator.comparing((Loan l) -> l.dueDate));
        return out;
    }

    /** Total you still have to pay back. */
    public double totalOwed() {
        double s = 0;
        for (Loan l : loans) if (l.isBorrowed()) s += l.remaining();
        return s;
    }

    /** Total others still have to pay you. */
    public double totalReceivable() {
        double s = 0;
        for (Loan l : loans) if (!l.isBorrowed()) s += l.remaining();
        return s;
    }

    // ------------------------------------------------------------------ analytics
    // Loan money moves (borrow / lend / repay) change your balance but are not "income" or "spending".

    public double balance() {
        double b = 0;
        for (Transaction t : transactions) b += t.isIncome() ? t.amount : -t.amount;
        return b;
    }

    public double income(YearMonth m) { return sum(m, true); }
    public double expense(YearMonth m) { return sum(m, false); }

    private double sum(YearMonth m, boolean income) {
        double s = 0;
        for (Transaction t : transactions) {
            if (t.isLoanRelated() || t.isIncome() != income) continue;
            if (m == null || YearMonth.from(t.date).equals(m)) s += t.amount;
        }
        return s;
    }

    /** Expense totals per category (loans excluded), largest first. from/to may be null for "all time". */
    public LinkedHashMap<String, Double> expenseByCategory(LocalDate from, LocalDate to) {
        Map<String, Double> m = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.isLoanRelated() || t.isIncome()) continue;
            if (from != null && t.date.isBefore(from)) continue;
            if (to != null && t.date.isAfter(to)) continue;
            m.merge(t.category, t.amount, Double::sum);
        }
        List<Map.Entry<String, Double>> e = new ArrayList<>(m.entrySet());
        e.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, Double> x : e) out.put(x.getKey(), x.getValue());
        return out;
    }

    public Set<String> categories() {
        Set<String> s = new TreeSet<>();
        for (Transaction t : transactions) if (!t.isLoanRelated()) s.add(t.category);
        return s;
    }

    public void resetAll() {
        transactions.clear();
        loans.clear();
        goals.clear();
        changed();
    }
}
