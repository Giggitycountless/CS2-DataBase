package com.counterstrike.app.ui;

import com.counterstrike.app.CounterStrikeApp;
import com.counterstrike.app.repository.AppRepository;
import com.counterstrike.app.repository.HomeSummary;
import com.counterstrike.app.repository.TableData;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainFrame extends JFrame {

    // ── CS2-inspired dark palette (package-visible so sibling panels can reuse it) ──
    static final Color ACCENT       = new Color(0xDE9B35);  // gold/amber
    static final Color ACCENT_DIM   = new Color(0x8B6914);
    static final Color BG_DARKER    = new Color(0x1A1A24);
    static final Color BG_CARD      = new Color(0x242436);
    static final Color TEXT_PRIMARY = new Color(0xE8E8E8);
    static final Color TEXT_MUTED   = new Color(0x8E8E9A);
    static final Color BORDER_SUBTLE = new Color(0x363650);
    static final Color TABLE_ALT    = new Color(0x1E1E2E);
    static final Color TABLE_HEADER_BG = new Color(0x2A2A40);
    static final Color METRIC_BG    = new Color(0x2A2A42);

    private static final String HOME = "Home";
    private static final String PLAYERS = "Players";
    private static final String TEAMS = "Teams";
    private static final String MATCHES = "Matches";
    private static final String TOURNAMENTS = "Tournaments";
    private static final String MANAGE = "Manage";
    private static final String ANALYTICS = "Analytics";

    private final AppRepository repository;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JTextField searchField = new JTextField();
    private final JLabel statusLabel = new JLabel("Ready");

    // cache nav buttons so we can highlight active one
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    // widgets
    private final JComboBox<String> homeCategory = new JComboBox<>(new String[]{"Person", "Team", "Tournament"});
    private final JLabel playerCount = metricLabel();
    private final JLabel teamCount = metricLabel();
    private final JLabel matchCount = metricLabel();
    private final JLabel tournamentCount = metricLabel();
    private final JLabel topPlayerName = cardTitleLabel("No players");
    private final JLabel topPlayerDetail = cardDetailLabel("");
    private final JLabel trendingTeamName = cardTitleLabel("No teams");
    private final JLabel trendingTeamDetail = cardDetailLabel("");
    private final JTable homeTable = table();
    private final JTable playersTable = table();
    private final JTable teamsTable = table();
    private final JTable matchesTable = table();
    private final JTable tournamentsTable = table();

    private String currentPage = HOME;

    public MainFrame(AppRepository repository, String databaseUrl) {
        this.repository = repository;

        setTitle("Counter-Strike Database Browser");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 660));
        setSize(1200, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARKER);
        setLayout(new BorderLayout());

        add(header(databaseUrl), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);

        showPage(HOME);
    }

    // ── Header: nav + database info + search ──
    private JPanel header(String databaseUrl) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(14, 20, 10, 20));
        header.setBackground(BG_DARKER);

        // Navigation row
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nav.setOpaque(false);
        addNavButton(nav, HOME);
        addNavButton(nav, PLAYERS);
        addNavButton(nav, TEAMS);
        addNavButton(nav, MATCHES);
        addNavButton(nav, TOURNAMENTS);
        addNavButton(nav, MANAGE);
        addNavButton(nav, ANALYTICS);

        JLabel dbLabel = new JLabel(repository.dbLabel() + ": " + databaseUrl);
        dbLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        dbLabel.setForeground(TEXT_MUTED);
        dbLabel.setFont(dbLabel.getFont().deriveFont(11f));

        // Search bar — Switch DB lives here so the nav row has full width
        JButton switchBtn = new JButton("Switch DB");
        styleActionButton(switchBtn);
        switchBtn.addActionListener(e -> CounterStrikeApp.connect(MainFrame.this));

        JPanel search = new JPanel(new BorderLayout(8, 0));
        search.setBorder(new EmptyBorder(14, 0, 0, 0));
        search.setOpaque(false);
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setForeground(TEXT_MUTED);
        JButton refresh = new JButton("Refresh");
        styleActionButton(refresh);

        JPanel searchRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchRight.setOpaque(false);
        searchRight.add(switchBtn);
        searchRight.add(refresh);

        search.add(searchLabel, BorderLayout.WEST);
        search.add(searchField, BorderLayout.CENTER);
        search.add(searchRight, BorderLayout.EAST);

        searchField.addActionListener(event -> reloadCurrentPage());
        refresh.addActionListener(event -> reloadCurrentPage());

        header.add(nav, BorderLayout.WEST);
        header.add(dbLabel, BorderLayout.EAST);
        header.add(search, BorderLayout.SOUTH);
        return header;
    }

    private void addNavButton(JPanel nav, String page) {
        JButton button = new JButton(page);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(page.equals(TOURNAMENTS) ? 140 : 108, 36));
        button.addActionListener(event -> showPage(page));
        navButtons.put(page, button);
        nav.add(button);
    }

    // ── Content: card layout for each page ──
    private JPanel content() {
        cards.add(homePanel(), HOME);
        cards.add(tablePanel("Players", playersTable), PLAYERS);
        cards.add(teamsPanel(), TEAMS);
        cards.add(tablePanel("Matches", matchesTable), MATCHES);
        cards.add(tablePanel("Tournaments", tournamentsTable), TOURNAMENTS);
        cards.add(new CrudPanel(repository), MANAGE);
        cards.add(new AnalyticsPanel(repository), ANALYTICS);
        cards.setBackground(BG_DARKER);
        return cards;
    }

    // ── Home page ──
    private JPanel homePanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBorder(new EmptyBorder(16, 24, 16, 24));
        panel.setBackground(BG_DARKER);
        panel.setOpaque(true);

        JLabel title = pageTitle("Home");
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);
        center.add(homeControlsAndResults());
        center.add(homeTrending());
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel homeControlsAndResults() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(14, 16, 0, 16));
        JLabel catLabel = new JLabel("Category");
        catLabel.setForeground(TEXT_MUTED);
        top.add(catLabel, BorderLayout.WEST);
        top.add(homeCategory, BorderLayout.CENTER);

        homeCategory.addActionListener(event -> reloadCurrentPage());

        JPanel metrics = new JPanel(new GridLayout(2, 2, 10, 10));
        metrics.setBorder(new EmptyBorder(14, 16, 14, 16));
        metrics.setOpaque(false);
        metrics.add(metricCard("Players", playerCount));
        metrics.add(metricCard("Teams", teamCount));
        metrics.add(metricCard("Matches", matchCount));
        metrics.add(metricCard("Tournaments", tournamentCount));

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(metrics, BorderLayout.CENTER);

        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(homeTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel metricCard(String label, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(METRIC_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                new EmptyBorder(12, 16, 12, 16)));
        JLabel name = new JLabel(label);
        name.setForeground(TEXT_MUTED);
        name.setFont(name.getFont().deriveFont(11f));
        panel.add(name, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JPanel homeTrending() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 16));
        panel.setOpaque(false);
        panel.add(trendingCard("Trending Player", topPlayerName, topPlayerDetail));
        panel.add(trendingCard("Trending Team", trendingTeamName, trendingTeamDetail));
        return panel;
    }

    private JPanel trendingCard(String label, JLabel name, JLabel detail) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                new EmptyBorder(28, 28, 28, 28)));

        JLabel heading = new JLabel(label);
        heading.setForeground(TEXT_MUTED);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 12f));
        panel.add(heading, BorderLayout.NORTH);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 8));
        text.setOpaque(false);
        text.add(name);
        text.add(detail);
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    // ── Generic table page (Players, Matches, Tournaments) ──
    private JPanel tablePanel(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(16, 24, 16, 24));
        panel.setBackground(BG_DARKER);

        JLabel t = pageTitle(title);
        t.setForeground(TEXT_PRIMARY);
        panel.add(t, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ── Teams page (has Add/Update buttons) ──
    private JPanel teamsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(16, 24, 16, 24));
        panel.setBackground(BG_DARKER);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel t = pageTitle("Teams");
        t.setForeground(TEXT_PRIMARY);
        top.add(t, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton addButton = new JButton("Add Team");
        JButton updateButton = new JButton("Update Selected");
        styleActionButton(addButton);
        styleActionButton(updateButton);
        addButton.addActionListener(event -> showTeamDialog(false, "", ""));
        updateButton.addActionListener(event -> updateSelectedTeam());
        actions.add(addButton);
        actions.add(updateButton);
        top.add(actions, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(teamsTable), BorderLayout.CENTER);
        return panel;
    }

    private void styleActionButton(JButton btn) {
        btn.setBackground(ACCENT_DIM);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
    }

    // ── Reusable widgets ──
    private JLabel pageTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 26f));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private JTable table() {
        JTable table = new JTable(new ReadOnlyTableModel());
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(ACCENT_DIM);
        table.setSelectionForeground(TEXT_PRIMARY);

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BG_CARD : TABLE_ALT);
                }
                setBorder(new EmptyBorder(4, 12, 4, 12));
                return c;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TEXT_MUTED);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_DIM));

        return table;
    }

    // ── Footer / status bar ──
    private JPanel footer() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(new EmptyBorder(8, 20, 10, 20));
        footer.setBackground(BG_DARKER);
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        footer.add(statusLabel, BorderLayout.WEST);
        return footer;
    }

    // ── Navigation logic ──
    private void showPage(String page) {
        currentPage = page;
        cardLayout.show(cards, page);

        // Highlight active button, dim others — CS2 tab style
        navButtons.forEach((name, button) -> {
            if (name.equals(page)) {
                button.setBackground(ACCENT);
                button.setForeground(Color.BLACK);
                button.setEnabled(true);
            } else {
                button.setBackground(null);          // FlatLaf default
                button.setForeground(null);
                button.setEnabled(true);
            }
        });

        reloadCurrentPage();
    }

    private void reloadCurrentPage() {
        switch (currentPage) {
            case HOME -> loadHome();
            case PLAYERS -> loadTable(playersTable, "Players", repository::findTopPlayers);
            case TEAMS -> loadTable(teamsTable, "Teams", repository::findTrendingTeams);
            case MATCHES -> loadTable(matchesTable, "Matches", repository::findTeamMatchHistory);
            case TOURNAMENTS -> loadTable(tournamentsTable, "Tournaments", repository::findTournamentMatchSummary);
            case MANAGE, ANALYTICS -> setStatus(currentPage + " | use the controls on this page");
            default -> throw new IllegalStateException("Unknown page: " + currentPage);
        }
    }

    // ── Data loading ──
    private void loadHome() {
        String category = String.valueOf(homeCategory.getSelectedItem());
        String search = searchField.getText();
        setStatus("Loading Home...");

        SwingWorker<HomeLoadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HomeLoadResult doInBackground() throws Exception {
                HomeSummary summary = repository.loadHomeSummary();
                TableData tableData = switch (category) {
                    case "Team" -> repository.findTeamsBasic(search);
                    case "Tournament" -> repository.findTournamentsBasic(search);
                    default -> repository.findPeople(search);
                };
                return new HomeLoadResult(summary, tableData);
            }

            @Override
            protected void done() {
                try {
                    HomeLoadResult result = get();
                    updateSummary(result.summary());
                    setTableData(homeTable, result.tableData());
                    setStatus("Home | " + category + " results: " + result.tableData().rowCount());
                } catch (Exception exception) {
                    showQueryError(exception);
                }
            }
        };
        worker.execute();
    }

    private void loadTable(JTable table, String page, TableLoader loader) {
        String search = searchField.getText();
        setStatus("Loading " + page + "...");

        SwingWorker<TableData, Void> worker = new SwingWorker<>() {
            @Override
            protected TableData doInBackground() throws Exception {
                return loader.load(search);
            }

            @Override
            protected void done() {
                try {
                    TableData data = get();
                    setTableData(table, data);
                    setStatus(page + " | rows: " + data.rowCount());
                } catch (Exception exception) {
                    showQueryError(exception);
                }
            }
        };
        worker.execute();
    }

    // ── Team CRUD ──
    private void updateSelectedTeam() {
        int selectedRow = teamsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a team row first.", "No Team Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = teamsTable.convertRowIndexToModel(selectedRow);
        String teamName = valueAsText(teamsTable.getModel().getValueAt(modelRow, 0));
        String region = valueAsText(teamsTable.getModel().getValueAt(modelRow, 1));
        showTeamDialog(true, teamName, region);
    }

    private void showTeamDialog(boolean update, String initialTeamName, String initialRegion) {
        JTextField teamNameField = new JTextField(initialTeamName, 24);
        JTextField regionField = new JTextField(initialRegion, 24);
        teamNameField.setEditable(!update);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Team Name"));
        form.add(teamNameField);
        form.add(new JLabel("Region"));
        form.add(regionField);

        int result = JOptionPane.showConfirmDialog(
                this, form,
                update ? "Update Team Region" : "Add Team",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String teamName = teamNameField.getText().trim();
        String region = regionField.getText().trim();
        String validationMessage = validateTeamInput(teamName, region);
        if (validationMessage != null) {
            JOptionPane.showMessageDialog(this, validationMessage, "Invalid Team Data", JOptionPane.WARNING_MESSAGE);
            showTeamDialog(update, teamName, region);
            return;
        }
        saveTeam(update, teamName, region);
    }

    private void saveTeam(boolean update, String teamName, String region) {
        setStatus(update ? "Updating team..." : "Adding team...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (update) repository.updateTeamRegion(teamName, region);
                else repository.addTeam(teamName, region);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus(update ? "Updated team: " + teamName : "Added team: " + teamName);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            update ? "Team region updated." : "Team added.",
                            "Team Saved", JOptionPane.INFORMATION_MESSAGE);
                    reloadCurrentPage();
                } catch (Exception exception) {
                    showQueryError(exception);
                }
            }
        };
        worker.execute();
    }

    private String validateTeamInput(String teamName, String region) {
        if (teamName.isBlank()) return "Team name is required.";
        if (teamName.length() > 100) return "Team name must be 100 characters or fewer.";
        if (region.isBlank()) return "Region is required.";
        if (region.length() > 50) return "Region must be 50 characters or fewer.";
        return null;
    }

    // ── Helpers ──
    private void updateSummary(HomeSummary summary) {
        playerCount.setText(String.valueOf(summary.playerCount()));
        teamCount.setText(String.valueOf(summary.teamCount()));
        matchCount.setText(String.valueOf(summary.matchCount()));
        tournamentCount.setText(String.valueOf(summary.tournamentCount()));
        topPlayerName.setText(summary.topPlayerName());
        topPlayerDetail.setText(summary.topPlayerDetail());
        trendingTeamName.setText(summary.trendingTeamName());
        trendingTeamDetail.setText(summary.trendingTeamDetail());
    }

    private void setTableData(JTable table, TableData data) {
        ReadOnlyTableModel model = (ReadOnlyTableModel) table.getModel();
        model.setData(data);
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showQueryError(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        if (cause instanceof SQLException sqlException) cause = sqlException;
        setStatus("Error: " + cause.getMessage());
        JOptionPane.showMessageDialog(this, cause.getMessage(), "Database Query Error", JOptionPane.ERROR_MESSAGE);
    }

    private static String valueAsText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static JLabel metricLabel() {
        JLabel label = new JLabel("0");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 28f));
        label.setForeground(ACCENT);
        return label;
    }

    private static JLabel cardTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 26f));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private static JLabel cardDetailLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(13f));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    // ── Inner types ──
    @FunctionalInterface
    private interface TableLoader {
        TableData load(String search) throws Exception;
    }

    private record HomeLoadResult(HomeSummary summary, TableData tableData) {}
}
