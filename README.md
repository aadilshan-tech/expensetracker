# SpendWise

A modern dark-themed desktop expense tracker (Java Swing + FlatLaf) with **borrow / lend tracking and payback reminders**.

## Features

- **Dashboard** – balance, monthly income / expenses / savings rate with month-over-month change, 6-month cash-flow chart, spending donut, recent transactions and loan summary.
- **Transactions** – add, edit, delete, search, filter by type/category, sort by any column, export to CSV.
- **Borrowed & Lent** – record money you borrowed or lent, each with a **required payback date**.
  - Partial or full payments, with status: *Overdue*, *Due today*, *Due soon*, *Active*, *Settled*.
  - Optionally mirrors the money in your balance (loan money is never counted as income or spending).
  - **Reminders**: a pop-up when the app opens, a red badge on the sidebar, a dashboard banner, and system-tray
    notifications every 10 minutes while the app is running.
  - **Add to calendar (.ics)** – alerts at 9 AM the day before and on the due date, even when SpendWise is closed.
- **Categories** – spending breakdown by period (this month / last 30 days / this year / all time).
- **Reports** – yearly monthly summary, chart and CSV export.
- **Goals** – savings goals with progress and "save X per month" guidance.
- **Settings** – name, currency symbol, reminder lead time, tray behaviour, export, reset.

## Run it

Requires **Java 17+**.

```bash
mvn package
java -jar target/SpendWise.jar
```

Or open the folder in IntelliJ IDEA (as a Maven project) and run `spendwise.Main`.

Every push to `main` builds `SpendWise.jar` automatically (GitHub → Actions → latest run → *Artifacts*).
Push a tag like `v2.0.0` to publish the jar on the repository's **Releases** page.

## Your data

Stored locally in `~/.spendwise/` (CSV + a settings file). Nothing is sent anywhere.
If a `spendwise_data.csv` from the first version is found next to the app on first launch, it is imported automatically.

## Project layout

```
src/spendwise/   Main, MainFrame (window + sidebar), *Screen (pages), Dialogs, Charts, DataStore, ReminderService, ...
```
