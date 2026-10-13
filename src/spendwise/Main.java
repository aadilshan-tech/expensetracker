package spendwise;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.Color;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#10b981"));
        try { FlatDarkLaf.setup(); } catch (Exception ignored) {}
        theme();

        SwingUtilities.invokeLater(() -> {
            DataStore store = new DataStore();
            MainFrame frame = new MainFrame(store);
            frame.setVisible(true);
            frame.startReminders();
        });
    }

    /** Tunes the dark look-and-feel towards the SpendWise palette. */
    private static void theme() {
        Color input = new Color(0x0E1627);
        UIManager.put("Panel.background", Theme.BG);
        UIManager.put("OptionPane.background", Theme.CARD);
        UIManager.put("OptionPane.messageForeground", Theme.TEXT);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.borderColor", Theme.BORDER);
        UIManager.put("Component.focusColor", Theme.GREEN);
        for (String k : new String[]{"TextField", "FormattedTextField", "PasswordField", "ComboBox", "Spinner"})
            UIManager.put(k + ".background", input);
        UIManager.put("ComboBox.buttonBackground", input);
        UIManager.put("Spinner.buttonBackground", input);
        UIManager.put("ComboBox.popupBackground", Theme.CARD);
        UIManager.put("ToolTip.background", Theme.CARD_ALT);
    }
}
