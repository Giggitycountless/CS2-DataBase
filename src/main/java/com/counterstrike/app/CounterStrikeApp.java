package com.counterstrike.app;

import com.counterstrike.app.config.DatabaseConfig;
import com.counterstrike.app.db.Database;
import com.counterstrike.app.repository.CounterStrikeRepository;
import com.counterstrike.app.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class CounterStrikeApp {
    private CounterStrikeApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CounterStrikeApp::start);
    }

    private static void start() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
