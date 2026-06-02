package com.counterstrike.app;

import com.counterstrike.app.config.DatabaseConfig;
import com.counterstrike.app.db.Database;
import com.counterstrike.app.db.MongoDatabase;
import com.counterstrike.app.repository.AppRepository;
import com.counterstrike.app.repository.CounterStrikeRepository;
import com.counterstrike.app.repository.MongoCounterStrikeRepository;
import com.counterstrike.app.ui.MainFrame;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class CounterStrikeApp {
    private CounterStrikeApp() {}

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("Component.focusWidth", 1.5f);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        SwingUtilities.invokeLater(CounterStrikeApp::start);
    }

    private static void start() {
        String[] options = {"Oracle (Relational)", "MongoDB (NoSQL)"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Select the database backend to connect to:",
                "CS2 Database Browser — Connect",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice < 0) return;

        try {
            AppRepository repository;
            String dbInfo;
            if (choice == 0) {
                DatabaseConfig config = DatabaseConfig.load();
                Database database = new Database(config);
                database.testConnection();
                repository = new CounterStrikeRepository(database);
                dbInfo = config.url();
            } else {
                String uri = (String) JOptionPane.showInputDialog(
                        null,
                        "MongoDB connection URI:",
                        "MongoDB Connection",
                        JOptionPane.PLAIN_MESSAGE,
                        null, null,
                        "mongodb://root:example@localhost:27017");
                if (uri == null || uri.isBlank()) return;
                MongoDatabase mongo = new MongoDatabase(uri.trim(), "cs_small");
                mongo.testConnection();
                repository = new MongoCounterStrikeRepository(mongo);
                dbInfo = uri.trim() + "/cs2";
            }
            MainFrame frame = new MainFrame(repository, dbInfo);
            frame.setVisible(true);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(
                    null,
                    exception.getMessage(),
                    "Counter-Strike Browser — Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
