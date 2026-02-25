import com.sun.management.OperatingSystemMXBean;

import java.io.File;
import java.lang.management.ManagementFactory;

public class ServerNode {
    private final File diskRoot;
    private final OperatingSystemMXBean osBean;

    public ServerNode(String diskPath) {
        this.diskRoot = new File(diskPath);
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    public SystemMetrics captureMetrics() {
        double cpuLoad = percent(osBean.getCpuLoad());

        long ramTotal = osBean.getTotalMemorySize();
        long ramFree = osBean.getFreeMemorySize();
        long ramUsed = Math.max(0L, ramTotal - ramFree);

        File drive = diskRoot.exists() ? diskRoot : File.listRoots()[0];
        long diskTotal = drive.getTotalSpace();
        long diskFree = drive.getFreeSpace();
        long diskUsed = Math.max(0L, diskTotal - diskFree);

        return new SystemMetrics(
                cpuLoad, ramTotal, ramUsed, ramFree,
                diskTotal, diskUsed, diskFree
        );
    }

    private static double percent(double fraction) {
        if (fraction < 0) return 0.0;
        return Math.max(0.0, Math.min(100.0, fraction * 100.0));
    }
}
