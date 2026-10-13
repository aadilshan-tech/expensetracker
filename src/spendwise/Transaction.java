package spendwise;

import java.time.LocalDate;
import java.util.UUID;

/** A single income or expense entry. */
public class Transaction {
    public static final String INCOME = "Income";
    public static final String EXPENSE = "Expense";
    /** Category used for money that is borrowed / lent / paid back. Excluded from income & expense stats. */
    public static final String LOAN_CATEGORY = "Loan";

    public String id;
    public LocalDate date;
    public String category;
    public String description;
    public double amount;
    public String type;
    /** Id of the loan this entry belongs to (empty when not loan related). */
    public String loanId = "";

    public Transaction(String id, LocalDate date, String category, String description,
                       double amount, String type, String loanId) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.loanId = loanId == null ? "" : loanId;
    }

    public Transaction(LocalDate date, String category, String description, double amount, String type) {
        this(UUID.randomUUID().toString().substring(0, 8), date, category, description, amount, type, "");
    }

    public boolean isIncome() { return INCOME.equals(type); }
    public boolean isLoanRelated() { return !loanId.isEmpty() || LOAN_CATEGORY.equals(category); }
}
