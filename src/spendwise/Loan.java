package spendwise;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Money borrowed from someone (you must pay back) or lent to someone (you get it back). */
public class Loan {
    public enum Direction { BORROWED, LENT }
    public enum Status { OVERDUE, DUE_TODAY, DUE_SOON, ACTIVE, SETTLED }

    public String id;
    public String person;
    public Direction direction;
    public double amount;
    public double paid;
    public LocalDate startDate;
    public LocalDate dueDate;
    public String note;
    /** True when the loan and its payments are mirrored in the balance as transactions. */
    public boolean linked;
    public LocalDate settledDate;

    public Loan(String id, String person, Direction direction, double amount, double paid,
                LocalDate startDate, LocalDate dueDate, String note, boolean linked, LocalDate settledDate) {
        this.id = id;
        this.person = person;
        this.direction = direction;
        this.amount = amount;
        this.paid = paid;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.note = note == null ? "" : note;
        this.linked = linked;
        this.settledDate = settledDate;
    }

    public static Loan create(String person, Direction d, double amount, LocalDate start, LocalDate due,
                              String note, boolean linked) {
        return new Loan(UUID.randomUUID().toString().substring(0, 8), person, d, amount, 0, start, due, note, linked, null);
    }

    public double remaining() { return Math.max(0, amount - paid); }
    public boolean isSettled() { return amount - paid < 0.005; }
    public boolean isBorrowed() { return direction == Direction.BORROWED; }

    /** Days from today until the due date (negative when overdue). */
    public long daysUntilDue(LocalDate today) { return ChronoUnit.DAYS.between(today, dueDate); }

    public Status status(LocalDate today, int leadDays) {
        if (isSettled()) return Status.SETTLED;
        long d = daysUntilDue(today);
        if (d < 0) return Status.OVERDUE;
        if (d == 0) return Status.DUE_TODAY;
        if (d <= leadDays) return Status.DUE_SOON;
        return Status.ACTIVE;
    }

    public static String statusText(Loan l, LocalDate today, int leadDays) {
        Status s = l.status(today, leadDays);
        long d = l.daysUntilDue(today);
        switch (s) {
            case SETTLED:   return "Settled";
            case OVERDUE:   return "Overdue " + (-d) + (-d == 1 ? " day" : " days");
            case DUE_TODAY: return "Due today";
            case DUE_SOON:  return d == 1 ? "Due tomorrow" : "Due in " + d + " days";
            default:        return d + " days left";
        }
    }
}
