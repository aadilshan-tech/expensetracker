package spendwise;

import java.time.LocalDate;
import java.util.UUID;

/** A savings goal. Tracks its own saved amount (independent of the main balance). */
public class Goal {
    public String id;
    public String name;
    public double target;
    public double saved;
    public LocalDate targetDate; // may be null

    public Goal(String id, String name, double target, double saved, LocalDate targetDate) {
        this.id = id;
        this.name = name;
        this.target = target;
        this.saved = saved;
        this.targetDate = targetDate;
    }

    public static Goal create(String name, double target, LocalDate targetDate) {
        return new Goal(UUID.randomUUID().toString().substring(0, 8), name, target, 0, targetDate);
    }

    public double progress() { return target <= 0 ? 0 : Math.min(1.0, saved / target); }
    public boolean reached() { return saved >= target; }
}
