package spendwise;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** Base class of every page shown in the main area. */
public abstract class Screen extends JPanel {
    protected final MainFrame app;
    protected final DataStore store;

    protected Screen(MainFrame app, DataStore store) {
        super(new BorderLayout());
        this.app = app;
        this.store = store;
        setOpaque(false);
        setBorder(new EmptyBorder(28, 32, 24, 32));
    }

    /** Re-read data from the store and update the display. */
    public abstract void refresh();

    protected JPanel header(String title, JLabel subtitle, JComponent... actions) {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 20, 0));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 4));
        left.setOpaque(false);
        left.add(Ui.title(title));
        if (subtitle != null) left.add(subtitle);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        for (JComponent c : actions) right.add(c);
        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    /** A scrollable column that follows the viewport width (and stretches when there is room). */
    public static class Page extends JPanel implements Scrollable {
        public Page(LayoutManager lm) { super(lm); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /** Title, big number and a small note. */
    public static class StatCard extends Ui.Card {
        private final JLabel title, value, sub;
        public StatCard(String titleText, Color accent) {
            super();
            accent(accent);
            setLayout(new GridLayout(0, 1, 0, 3));
            title = Ui.muted(titleText);
            value = Ui.label(" ", 26f, Font.BOLD, Theme.TEXT);
            sub = Ui.label(" ", 12.5f, Font.PLAIN, Theme.MUTED);
            add(title); add(value); add(sub);
        }
        public void set(String v, String note, Color noteColor) {
            value.setText(v);
            sub.setText(note == null || note.isEmpty() ? " " : note);
            sub.setForeground(noteColor);
        }
        public void setTitle(String t) { title.setText(t); }
    }

    // ------------------------------------------------------------------ CSV export

    private static String q(String s) {
        if (s == null) return "";
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    protected void exportCsv(String defaultName, List<String[]> rows) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultName));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM so Excel reads the currency symbol correctly
        for (String[] r : rows) {
            for (int i = 0; i < r.length; i++) sb.append(i > 0 ? "," : "").append(q(r[i]));
            sb.append("\r\n");
        }
        try {
            Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "Saved to\n" + f.getAbsolutePath(), "Export complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the file:\n" + ex.getMessage(), "Export failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
