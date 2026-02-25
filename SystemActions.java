import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SystemActions {
    private final Supplier<SystemMetrics> metricsSupplier;
    private final Consumer<String> output;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SystemActions(Supplier<SystemMetrics> metricsSupplier, Consumer<String> output) {
        this.metricsSupplier = metricsSupplier;
        this.output = output;
    }

    public void killProcess(String processName) {
        if (isBlank(processName)) {
            output.accept("[ERROR] Process name is required.");
            return;
        }
        runCommand("Kill Process", "taskkill /F /IM " + processName.trim());
    }

    public void cleanTempFiles() {
        String tempPath = System.getenv("TEMP");
        if (tempPath == null || tempPath.isEmpty()) {
            output.accept("[ERROR] TEMP environment variable is not set.");
            return;
        }
        java.io.File tempDir = new java.io.File(tempPath);
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            output.accept("[ERROR] TEMP directory not found: " + tempDir.getAbsolutePath());
            return;
        }

        deleteRecursive(tempDir);
        output.accept("[Success] Temp files cleanup completed. Locked files were skipped.");
    }

    /** Recursive cleanup using File.listFiles(). Ignores any exception and continues. */
    private void deleteRecursive(java.io.File dir) {
        java.io.File[] children = null;
        try {
            children = dir.listFiles();
        } catch (Exception e) {
            return;
        }
        if (children == null) return;
        for (java.io.File f : children) {
            try {
                if (f.isDirectory()) {
                    deleteRecursive(f);
                }
                f.delete();
            } catch (Exception e) {
                // ignore and continue to next file
            }
        }
    }

    public void ping(String target) {
        if (isBlank(target)) {
            output.accept("[ERROR] IP/Domain is required.");
            return;
        }
        runCommand("Ping Test", "ping " + target.trim());
    }

    public void exportHealthReport() {
        SystemMetrics m = metricsSupplier.get();
        if (m == null) {
            output.accept("[ERROR] No live metrics available yet.");
            return;
        }

        Path report = Path.of(System.getProperty("user.home"), "Desktop", "Health_Report.txt");
        String content = ""
                + "=== Server Health Report ===" + System.lineSeparator()
                + "Generated: " + LocalDateTime.now().format(timeFmt) + System.lineSeparator()
                + "CPU Load: " + fmt(m.getCpuLoadPercent()) + "%" + System.lineSeparator()
                + "RAM Usage: " + fmt(m.getRamUsagePercent()) + "%" + System.lineSeparator()
                + "RAM Total/Used/Free (GB): " + gb(m.getRamTotalBytes()) + " / " + gb(m.getRamUsedBytes()) + " / " + gb(m.getRamFreeBytes()) + System.lineSeparator()
                + "Disk Usage: " + fmt(m.getDiskUsagePercent()) + "%" + System.lineSeparator()
                + "Disk Total/Used/Free (GB): " + gb(m.getDiskTotalBytes()) + " / " + gb(m.getDiskUsedBytes()) + " / " + gb(m.getDiskFreeBytes()) + System.lineSeparator();

        try {
            Files.writeString(report, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            output.accept("[OK] Report generated at: " + report);
        } catch (IOException ex) {
            output.accept("[ERROR] Could not export report: " + ex.getMessage());
        }
    }

    public void lockWorkstation() {
        runCommand("Lock Workstation", "rundll32.exe user32.dll,LockWorkStation");
    }

    public void runCustomCommand(String command) {
        if (isBlank(command)) {
            output.accept("[ERROR] Command is required.");
            return;
        }
        runCommand("Custom Command", command.trim());
    }

    public void disconnectNetwork() {
        runCommand("Disconnect Network", "ipconfig /release");
    }

    public void reconnectNetwork() {
        runCommand("Reconnect Network", "ipconfig /renew");
    }

    public void scanConnections() {
        runCommand("Active Connections", "netstat -ano");
    }

    private void runCommand(String title, String command) {
        output.accept("[" + title + "] cmd /c " + command);
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.accept(line);
                }
            }
            int code = process.waitFor();
            output.accept("[" + title + "] exit code: " + code);
        } catch (Exception ex) {
            output.accept("[" + title + "] error: " + ex.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String fmt(double value) {
        return String.format("%.1f", value);
    }

    private static String gb(long bytes) {
        return String.format("%.2f", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
