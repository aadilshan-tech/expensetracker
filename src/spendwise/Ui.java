package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Function;

/** Reusable widgets: cards, buttons, pills, date field, dialogs and table styling. */
public final class Ui {
    private Ui() {}

    private static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ------------------------------------------------------------------ labels

    public static JLabel label(String text, float size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JLabel title(String text) { return label(text, 28f, Font.BOLD, Theme.TEXT); }
    public static JLabel muted(String text) { return label(text, 13f, Font.PLAIN, Theme.MUTED); }

    // ------------------------------------------------------------------ card

    /** Rounded surface with a subtle border. */
    public static class Card extends JPanel {
        private final int radius;
        private Color fill;
        private Color accent; // optional colored left edge

        public Card() { this(18, Theme.CARD); }
        public Card(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
            setBorder(new EmptyBorder(18, 20, 18, 20));
        }
        public Card accent(Color c) { this.accent = c; repaint(); return this; }
        public Card fill(Color c) { this.fill = c; repaint(); return this; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, radius, radius);
            g.setColor(fill);
            g.fill(shape);
            if (accent != null) {
                g.setClip(shape);
                g.setColor(accent);
                g.fillRect(0, 0, 4, getHeight());
                g.setClip(null);
            }
            g.setColor(Theme.BORDER);
            g.draw(shape);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    // ------------------------------------------------------------------ buttons

    public enum Kind { PRIMARY, SECONDARY, DANGER, GHOST }

    public static class Btn extends JButton {
        private final Kind kind;
        private boolean hover, press;

        public Btn(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(Theme.font(Font.BOLD, 13f));
            setForeground(kind == Kind.PRIMARY ? new Color(0x04130D) : kind == Kind.DANGER ? Theme.RED : Theme.TEXT);
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; press = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { press = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { press = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            Color base;
            Color border = null;
            switch (kind) {
                case PRIMARY:   base = Theme.GREEN; break;
                case DANGER:    base = Theme.alpha(Theme.RED, hover ? 60 : 30); break;
                case GHOST:     base = hover ? Theme.alpha(Color.WHITE, 14) : Theme.alpha(Color.WHITE, 0); break;
                default:        base = hover ? Theme.CARD_ALT.brighter() : Theme.CARD_ALT; border = Theme.BORDER;
            }
            if (kind == Kind.PRIMARY && hover) base = base.brighter();
            if (press) base = base.darker();
            if (!isEnabled()) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g.setColor(base);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            if (border != null) {
                g.setColor(border);
                g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 12, 12));
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    public static Btn primary(String t) { return new Btn(t, Kind.PRIMARY); }
    public static Btn secondary(String t) { return new Btn(t, Kind.SECONDARY); }
    public static Btn danger(String t) { return new Btn(t, Kind.DANGER); }
    public static Btn ghost(String t) { return new Btn(t, Kind.GHOST); }

    // ------------------------------------------------------------------ pill

    public static class Pill extends JLabel {
        private Color color;
        public Pill(String text, Color color) {
            super(text, CENTER);
            this.color = color;
            setFont(Theme.font(Font.BOLD, 11.5f));
            setForeground(color);
            setBorder(new EmptyBorder(4, 12, 4, 12));
            setOpaque(false);
        }
        public void set(String text, Color c) { setText(text); color = c; setForeground(c); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(Theme.alpha(color, 36));
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    // ------------------------------------------------------------------ inputs

    public static JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.putClientProperty("JComponent.roundRect", true);
        f.setFont(Theme.font(Font.PLAIN, 14f));
        f.setPreferredSize(new Dimension(260, 38));
        return f;
    }

    public static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setFont(Theme.font(Font.PLAIN, 14f));
        c.putClientProperty("JComponent.roundRect", true);
        c.setPreferredSize(new Dimension(260, 38));
        return c;
    }

    /** Parses "1,250.50" / "₹ 500" style input. Returns NaN when it is not a number. */
    public static double parseAmount(String s) {
        if (s == null) return Double.NaN;
        String t = s.replace(",", "").replace(Theme.currency, "").trim();
        if (t.isEmpty()) return Double.NaN;
        try {
            double v = Double.parseDouble(t);
            return Double.isInfinite(v) ? Double.NaN : v;
        } catch (NumberFormatException e) { return Double.NaN; }
    }

    /** Date input with a spinner (type or use arrow keys). */
    public static class DateField extends JSpinner {
        public DateField(LocalDate initial) {
            super(new SpinnerDateModel(toDate(initial), null, null, java.util.Calendar.DAY_OF_MONTH));
            setEditor(new JSpinner.DateEditor(this, "dd MMM yyyy"));
            setFont(Theme.font(Font.PLAIN, 14f));
            setPreferredSize(new Dimension(160, 38));
        }
        public LocalDate getDate() {
            try { commitEdit(); } catch (java.text.ParseException ignored) {}
            return ((Date) getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        public void setDate(LocalDate d) { setValue(toDate(d)); }
        private static Date toDate(LocalDate d) { return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()); }
    }

    /** Simple label + input form layout. */
    public static class Form extends JPanel {
        private int row;
        public Form() {
            super(new GridBagLayout());
            setOpaque(false);
        }
        public Form row(String label, JComponent c) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0; g.gridy = row; g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(0, 0, 4, 0); g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            add(muted(label), g);
            g.gridy = row + 1; g.insets = new Insets(0, 0, 14, 0);
            add(c, g);
            row += 2;
            return this;
        }
    }

    // ------------------------------------------------------------------ dialogs

    /** Returns an error message to keep the dialog open, or null after the action succeeded. */
    public interface Validator { String run(); }

    public static void dialog(Window owner, String title, String subtitle, JComponent body,
                              String okText, Validator onOk) {
        dialog(owner, title, subtitle, body, okText, "Cancel", onOk);
    }

    public static void dialog(Window owner, String title, String subtitle, JComponent body,
                              String okText, String cancelText, Validator onOk) {
        JDialog d = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(26, 28, 22, 28));

        JPanel head = new JPanel(new GridLayout(0, 1, 0, 4));
        head.setOpaque(false);
        head.add(label(title, 20f, Font.BOLD, Theme.TEXT));
        if (subtitle != null) head.add(muted(subtitle));
        root.add(head, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);

        JLabel err = label(" ", 12.5f, Font.PLAIN, Theme.RED);
        Btn cancel = secondary(cancelText);
        Btn ok = primary(okText);
        JPanel foot = new JPanel(new BorderLayout(12, 0));
        foot.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);
        foot.add(err, BorderLayout.CENTER);
        foot.add(buttons, BorderLayout.EAST);
        root.add(foot, BorderLayout.SOUTH);

        cancel.addActionListener(e -> d.dispose());
        ok.addActionListener(e -> {
            String msg = onOk.run();
            if (msg == null) d.dispose(); else err.setText(msg);
        });
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.getRootPane().setDefaultButton(ok);

        d.setContentPane(root);
        d.pack();
        d.setSize(Math.max(d.getWidth(), 480), d.getHeight());
        d.setResizable(false);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    public static boolean confirm(Component parent, String title, String message, String okText) {
        Object[] opts = {okText, "Cancel"};
        return JOptionPane.showOptionDialog(parent, message, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, opts, opts[1]) == 0;
    }

    // ------------------------------------------------------------------ tables & scrolling

    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        if (c instanceof JTable) s.setColumnHeaderView(((JTable) c).getTableHeader());
        s.setBorder(null);
        s.setViewportBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.getVerticalScrollBar().setUnitIncrement(18);
        return s;
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(48);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(Theme.CARD);
        t.setForeground(Theme.TEXT);
        t.setSelectionBackground(Theme.alpha(Theme.GREEN, 45));
        t.setSelectionForeground(Theme.TEXT);
        t.setFont(Theme.font(Font.PLAIN, 13.5f));
        t.setFillsViewportHeight(true);
        t.setFocusable(true);
        t.setRowSelectionAllowed(true);
        t.setDefaultRenderer(Object.class, new Cell(false, false, null));
        t.setDefaultRenderer(Double.class, new Cell(true, false, null));
        t.setDefaultRenderer(LocalDate.class, new Cell(false, false, null));
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 40));
        h.setDefaultRenderer(new HeaderRenderer());
    }

    /** Right-aligns and (optionally) colors a column. Doubles are shown as money, dates as "13 Oct 2026". */
    public static void column(JTable t, int col, boolean right, Function<Object, Color> color) {
        t.getColumnModel().getColumn(col).setCellRenderer(new Cell(right, false, color));
    }

    /** Like column() but money values get an explicit + sign when positive. */
    public static void signedColumn(JTable t, int col, Function<Object, Color> color) {
        t.getColumnModel().getColumn(col).setCellRenderer(new Cell(true, true, color));
    }

    public static void pillColumn(JTable t, int col, Function<Object, Color> color) {
        t.getColumnModel().getColumn(col).setCellRenderer(new PillCell(color));
    }

    public static final java.time.format.DateTimeFormatter DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH);

    static class Cell extends DefaultTableCellRenderer {
        final boolean right, signed;
        private final Function<Object, Color> color;
        Cell(boolean right, boolean signed, Function<Object, Color> color) {
            this.right = right; this.signed = signed; this.color = color;
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, false, r, c);
            if (v instanceof Double) setText((signed && (Double) v > 0 ? "+" : "") + Theme.money((Double) v));
            else if (v instanceof LocalDate) setText(DATE_FMT.format((LocalDate) v));
            setBorder(new EmptyBorder(0, 16, 0, 16));
            setHorizontalAlignment(right ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBackground(sel ? t.getSelectionBackground() : t.getBackground());
            setForeground(color != null && v != null ? color.apply(v) : Theme.TEXT);
            return this;
        }
    }

    /** Draws a rounded "pill" label directly (no nested components, so it never depends on layout). */
    static class PillCell extends JComponent implements TableCellRenderer {
        private final Function<Object, Color> color;
        private String text = "";
        private Color pillColor = Theme.MUTED, rowBg = Theme.CARD;
        PillCell(Function<Object, Color> color) { this.color = color; setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            rowBg = sel ? t.getSelectionBackground() : t.getBackground();
            text = String.valueOf(v);
            pillColor = color.apply(v);
            return this;
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(Theme.CARD);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(rowBg);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(Theme.font(Font.BOLD, 11.5f));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(text) + 24, h = 24, x = 16, y = (getHeight() - h) / 2;
            g.setColor(Theme.alpha(pillColor, 40));
            g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
            g.setColor(pillColor);
            g.drawString(text, x + 12, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            super.getTableCellRendererComponent(t, v == null ? "" : v.toString().toUpperCase(), false, false, r, c);
            setFont(Theme.font(Font.BOLD, 11f));
            setForeground(Theme.MUTED);
            setBackground(Theme.CARD);
            setOpaque(true);
            setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    new EmptyBorder(0, 16, 0, 16)));
            TableCellRenderer cr = t.getColumnModel().getColumn(c).getCellRenderer();
            if (cr == null) cr = t.getDefaultRenderer(t.getColumnClass(c));
            boolean right = cr instanceof Cell && ((Cell) cr).right;
            setHorizontalAlignment(right ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }
}
