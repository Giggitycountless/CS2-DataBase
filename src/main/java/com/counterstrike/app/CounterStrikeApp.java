package com.counterstrike.app;

import com.counterstrike.app.config.DatabaseConfig;
import com.counterstrike.app.db.Database;
import com.counterstrike.app.repository.CounterStrikeRepository;
import com.counterstrike.app.ui.MainFrame;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class CounterStrikeApp {
    private CounterStrikeApp() {
    }

    public static void main(String[] args) {
        // FlatLaf dark theme — modern look, CS2 vibes
        FlatDarkLaf.setup();
        // Tweak a few FlatLaf defaults
        UIManager.put("Component.focusWidth", 1.5f);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));

        SwingUtilities.invokeLater(CounterStrikeApp::start);
    }

    private static void start() {
        try {
            DatabaseConfig config = DatabaseConfig.load();
            Database database = new Database(config);
            database.testConnection();

            CounterStrikeRepository repository = new CounterStrikeRepository(database);
            MainFrame frame = new MainFrame(repository, config.url());
            frame.setVisible(true);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(
                    null,
                    exception.getMessage(),
                    "Counter-Strike Browser Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
