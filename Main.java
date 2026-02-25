import javax.swing.*;
import java.awt.Color;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Locale;

public final class Main {
    private static final double ALERT_THRESHOLD = 85.0;
    private static final double REMEDIATION_THRESHOLD = 90.0;
    private static final String[] SIMULATED_PROCESSES = {
            "chrome.exe", "java.exe", "sqlservr.exe", "node.exe", "explorer.exe"
    };

    private static volatile boolean running = true;
    private static boolean cpuHigh;
    private static boolean ramHigh;
    private static boolean diskHigh;
    private static boolean remediationLogged;

    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UIManager.getLookAndFeelDefaults().put("Button.background", Color.decode("#22272e"));
                UIManager.getLookAndFeelDefaults().put("Button.foreground", Color.WHITE);
            } catch (Exception ignored) {
            }

            MonitorDashboard dashboard = new MonitorDashboard();
            Logger logger = new Logger(Path.of("Alerts_Log.txt"), dashboard::appendLog);
            ServerNode serverNode = new ServerNode("C:\\");
            AtomicReference<SystemMetrics> latestMetrics = new AtomicReference<>();
            SystemActions systemActions = new SystemActions(latestMetrics::get, dashboard::appendToolsOutput);
            dashboard.bindSystemActions(systemActions);

            dashboard.registerOnClose(() -> running = false);
            dashboard.setVisible(true);
            logger.info("Server Resource Alert Monitor started.");

            Thread monitorThread = new Thread(() -> monitorLoop(serverNode, dashboard, logger, latestMetrics), "monitor-thread");
            monitorThread.setDaemon(true);
            monitorThread.start();
        });
    }

    private static void monitorLoop(
            ServerNode serverNode,
            MonitorDashboard dashboard,
            Logger logger,
            AtomicReference<SystemMetrics> latestMetrics
    ) {
        while (running) {
            try {
                SystemMetrics metrics = serverNode.captureMetrics();
                latestMetrics.set(metrics);
                dashboard.refreshMetrics(metrics, ALERT_THRESHOLD);
                evaluate(metrics, logger);
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception ex) {
                logger.alert("Monitoring error: " + ex.getMessage());
                sleepQuietly();
            }
        }
        logger.info("Monitoring stopped.");
    }

    private static void evaluate(SystemMetrics m, Logger logger) {
        if (m.getCpuLoadPercent() >= ALERT_THRESHOLD) {
            if (!cpuHigh) {
                cpuHigh = true;
                logger.alert(String.format(Locale.US, "CPU critical at %.1f%%. Simulated heavy process: %s",
                        m.getCpuLoadPercent(), randomProcess()));
            }
            if (m.getCpuLoadPercent() >= REMEDIATION_THRESHOLD && !remediationLogged) {
                remediationLogged = true;
                logger.info("[Action Taken] Simulating automatic restart for heavy service...");
            }
        } else if (cpuHigh) {
            cpuHigh = false;
            remediationLogged = false;
            logger.info("CPU returned to normal range.");
        } else {
            remediationLogged = false;
        }

        if (m.getRamUsagePercent() >= ALERT_THRESHOLD) {
            if (!ramHigh) {
                ramHigh = true;
                logger.alert(String.format(Locale.US, "RAM critical at %.1f%%. Simulated heavy process: %s",
                        m.getRamUsagePercent(), randomProcess()));
            }
        } else if (ramHigh) {
            ramHigh = false;
            logger.info("RAM returned to normal range.");
        }

        if (m.getDiskUsagePercent() >= ALERT_THRESHOLD) {
            if (!diskHigh) {
                diskHigh = true;
                logger.alert(String.format(Locale.US, "Disk critical at %.1f%% on C: drive.", m.getDiskUsagePercent()));
            }
        } else if (diskHigh) {
            diskHigh = false;
            logger.info("Disk returned to normal range.");
        }
    }

    private static String randomProcess() {
        int i = (int) (Math.random() * SIMULATED_PROCESSES.length);
        return SIMULATED_PROCESSES[i];
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
