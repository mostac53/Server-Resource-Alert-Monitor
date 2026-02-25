import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.util.Locale;

public class MonitorDashboard extends JFrame {
    private static final Color APP_BG = Color.decode("#181b1f");
    private static final Color CARD_BG = Color.decode("#22272e");
    private static final Color PRIMARY_TEXT = Color.decode("#FFFFFF");
    private static final Color SECONDARY_TEXT = Color.decode("#A0AABF");
    private static final Color HIGHLIGHT_BLUE = Color.decode("#3498db");
    private static final Color WARNING_ORANGE = Color.decode("#f39c12");
    private static final Color CRITICAL_RED = Color.decode("#e74c3c");
    private static final Color TRACK_DARK = Color.decode("#14171a");

    private final JProgressBar cpuBar = createThinProgressBar();
    private final JProgressBar ramBar = createThinProgressBar();
    private final JProgressBar diskBar = createThinProgressBar();

    private final JLabel cpuBig = bigPercentLabel();
    private final JLabel ramBig = bigPercentLabel();
    private final JLabel diskBig = bigPercentLabel();

    private final JLabel cpuSub1 = subLabel();
    private final JLabel cpuSub2 = subLabel();
    private final JLabel ramSub1 = subLabel();
    private final JLabel ramSub2 = subLabel();
    private final JLabel diskSub1 = subLabel();
    private final JLabel diskSub2 = subLabel();

    private final JTextArea statusLogArea = new JTextArea(7, 70);
    private final JTextField processField = new JTextField();
    private final JTextField pingField = new JTextField();
    private final JTextField cmdField = new JTextField();
    private final JTextArea toolsOutputArea = new JTextArea(16, 70);
    private SystemActions systemActions;

    public MonitorDashboard() {
        super("Server Resource Alert Monitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(APP_BG);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(CARD_BG);
        tabs.setForeground(PRIMARY_TEXT);
        tabs.addTab("Overview", buildOverviewTab());
        tabs.addTab("Control Tools", buildControlToolsTab());
        // Custom tab labels: dark background + white text so tabs are always visible
        tabs.setTabComponentAt(0, tabLabel("Overview", CARD_BG));
        tabs.setTabComponentAt(1, tabLabel("Control Tools", CARD_BG));
        add(tabs, BorderLayout.CENTER);
    }

    public void refreshMetrics(SystemMetrics m, double threshold) {
        SwingUtilities.invokeLater(() -> {
            updateCard(cpuBar, cpuBig, m.getCpuLoadPercent());
            cpuSub1.setText(String.format(Locale.US, "System: %.1f%%", m.getCpuLoadPercent()));
            cpuSub2.setText("State: " + healthState(m.getCpuLoadPercent()));

            updateCard(ramBar, ramBig, m.getRamUsagePercent());
            ramSub1.setText(String.format(Locale.US, "Used: %.2f / %.2f GB", gb(m.getRamUsedBytes()), gb(m.getRamTotalBytes())));
            ramSub2.setText(String.format(Locale.US, "Free: %.2f GB", gb(m.getRamFreeBytes())));

            updateCard(diskBar, diskBig, m.getDiskUsagePercent());
            diskSub1.setText(String.format(Locale.US, "Used: %.2f / %.2f GB", gb(m.getDiskUsedBytes()), gb(m.getDiskTotalBytes())));
            diskSub2.setText(String.format(Locale.US, "Free: %.2f GB", gb(m.getDiskFreeBytes())));
        });
    }

    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            statusLogArea.append(line + System.lineSeparator());
            statusLogArea.setCaretPosition(statusLogArea.getDocument().getLength());
        });
    }

    public void appendToolsOutput(String line) {
        SwingUtilities.invokeLater(() -> {
            toolsOutputArea.append(line + System.lineSeparator());
            toolsOutputArea.setCaretPosition(toolsOutputArea.getDocument().getLength());
        });
    }

    public void registerOnClose(Runnable action) {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                action.run();
            }
        });
    }

    public void bindSystemActions(SystemActions actions) {
        this.systemActions = actions;
    }

    private JPanel buildOverviewTab() {
        JPanel root = panel(new BorderLayout(15, 15), APP_BG);
        root.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel cards = panel(new GridLayout(1, 3, 15, 15), APP_BG);
        cards.add(buildMetricCard("CPU Monitoring", cpuSub1, cpuSub2, cpuBig, cpuBar));
        cards.add(buildMetricCard("Memory (RAM) Monitoring", ramSub1, ramSub2, ramBig, ramBar));
        cards.add(buildMetricCard("Disk Space Monitoring", diskSub1, diskSub2, diskBig, diskBar));

        styleTextArea(statusLogArea, Color.decode("#000000"), Color.decode("#00FF00"));
        JScrollPane logPane = new JScrollPane(statusLogArea);
        logPane.setBorder(BorderFactory.createEmptyBorder());
        logPane.getViewport().setBackground(Color.decode("#000000"));

        root.add(cards, BorderLayout.NORTH);
        root.add(logPane, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildControlToolsTab() {
        JPanel root = panel(new BorderLayout(10, 10), APP_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel tools = panel(new GridLayout(8, 1, 8, 8), APP_BG);
        tools.add(actionRow("Process Killer", processField, button("Kill Process", () -> runAction(() -> systemActions.killProcess(processField.getText())))));
        tools.add(actionRow("Disk Junk Cleaner", null, button("Clean Temp Files", () -> runAction(systemActions::cleanTempFiles))));
        tools.add(actionRow("Ping Tool", pingField, button("Test Connection", () -> runAction(() -> systemActions.ping(pingField.getText())))));
        tools.add(actionRow("Export Health Report", null, button("Generate Report", () -> runAction(systemActions::exportHealthReport))));
        tools.add(actionRow("Lock Workstation", null, button("Lock Server", () -> runAction(systemActions::lockWorkstation))));
        tools.add(actionRow("Custom CMD Execution", cmdField, button("Run Command", () -> runAction(() -> systemActions.runCustomCommand(cmdField.getText())))));
        tools.add(networkRow());
        tools.add(actionRow("Active Connections", null, button("Scan Connections", () -> runAction(systemActions::scanConnections))));

        styleTextArea(toolsOutputArea, Color.decode("#000000"), Color.decode("#00FF00"));
        JScrollPane outputScroll = new JScrollPane(toolsOutputArea);
        outputScroll.setBorder(BorderFactory.createEmptyBorder());
        outputScroll.getViewport().setBackground(Color.decode("#000000"));

        root.add(tools, BorderLayout.NORTH);
        root.add(outputScroll, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildMetricCard(String title, JLabel sub1, JLabel sub2, JLabel big, JProgressBar bar) {
        JPanel card = panel(new BorderLayout(0, 12), CARD_BG);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(PRIMARY_TEXT);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

        JPanel center = panel(new BorderLayout(0, 8), CARD_BG);
        JPanel subs = panel(new GridLayout(2, 1), CARD_BG);
        subs.add(sub1);
        subs.add(sub2);
        center.add(subs, BorderLayout.NORTH);
        center.add(big, BorderLayout.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bar, BorderLayout.SOUTH);
        return card;
    }

    private JPanel actionRow(String title, JTextField field, JButton button) {
        JPanel row = panel(new BorderLayout(8, 8), CARD_BG);
        row.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(PRIMARY_TEXT);
        titleLabel.setPreferredSize(new Dimension(170, 28));
        row.add(titleLabel, BorderLayout.WEST);

        if (field != null) {
            styleField(field);
            row.add(field, BorderLayout.CENTER);
        }
        row.add(button, BorderLayout.EAST);
        return row;
    }

    private JPanel networkRow() {
        JPanel row = panel(new GridLayout(1, 3, 8, 8), CARD_BG);
        row.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel("Network Kill Switch");
        titleLabel.setForeground(PRIMARY_TEXT);
        row.add(titleLabel);
        row.add(button("Disconnect Network", () -> runAction(systemActions::disconnectNetwork)));
        row.add(button("Reconnect Network", () -> runAction(systemActions::reconnectNetwork)));
        return row;
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(false);
        button.setBackground(CARD_BG);
        button.setForeground(PRIMARY_TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SECONDARY_TEXT, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setUI(new DarkButtonUI());
        button.addActionListener(e -> action.run());
        return button;
    }

    private void runAction(Runnable action) {
        if (systemActions == null) {
            appendToolsOutput("[ERROR] System actions are not initialized.");
            return;
        }
        new Thread(action, "tool-action-thread").start();
    }

    private void updateCard(JProgressBar bar, JLabel bigPercent, double value) {
        int v = (int) Math.max(0, Math.min(100, Math.round(value)));
        bar.setValue(v);
        bar.setForeground(progressColor(v));
        bigPercent.setText(v + "%");
    }

    private static Color progressColor(int value) {
        if (value > 85) return CRITICAL_RED;
        if (value > 75) return WARNING_ORANGE;
        return HIGHLIGHT_BLUE;
    }

    private static String healthState(double value) {
        if (value > 85) return "Critical";
        if (value > 75) return "Warning";
        return "Normal";
    }

    private static JProgressBar createThinProgressBar() {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(100, 8));
        bar.setBackground(TRACK_DARK);
        bar.setForeground(HIGHLIGHT_BLUE);
        bar.setUI(new BasicProgressBarUI() {
            @Override
            protected Color getSelectionBackground() {
                return TRACK_DARK;
            }

            @Override
            protected Color getSelectionForeground() {
                return TRACK_DARK;
            }
        });
        return bar;
    }

    private static JLabel bigPercentLabel() {
        JLabel label = new JLabel("0%");
        label.setForeground(HIGHLIGHT_BLUE);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
        return label;
    }

    private static JLabel subLabel() {
        JLabel label = new JLabel("...");
        label.setForeground(SECONDARY_TEXT);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        return label;
    }

    private static void styleField(JTextField field) {
        field.setBackground(Color.decode("#2d333b"));
        field.setForeground(PRIMARY_TEXT);
        field.setCaretColor(PRIMARY_TEXT);
        field.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    private static void styleTextArea(JTextArea area, Color bg, Color fg) {
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(bg);
        area.setForeground(fg);
        area.setCaretColor(fg);
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private static JLabel tabLabel(String title, Color bg) {
        JLabel label = new JLabel("  " + title + "  ");
        label.setOpaque(true);
        label.setBackground(bg);
        label.setForeground(PRIMARY_TEXT);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        return label;
    }

    private static JPanel panel(LayoutManager layout, Color bg) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(bg);
        return panel;
    }

    private static double gb(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    /** Paints button with dark background and white text so Windows L&F cannot override. */
    private static class DarkButtonUI extends BasicButtonUI {
        private static final Color BG = Color.decode("#22272e");
        private static final Color FG = Color.decode("#FFFFFF");

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            int w = c.getWidth();
            int h = c.getHeight();
            g.setColor(BG);
            g.fillRect(0, 0, w, h);
            if (b.getBorder() != null) {
                g.setColor(Color.decode("#A0AABF"));
                g.drawRect(0, 0, w - 1, h - 1);
            }
            g.setColor(FG);
            g.setFont(b.getFont());
            FontMetrics fm = g.getFontMetrics();
            String label = b.getText();
            int x = (w - fm.stringWidth(label)) / 2;
            int y = (h + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(label, x, y);
        }
    }
}
