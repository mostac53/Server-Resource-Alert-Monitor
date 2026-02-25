public class SystemMetrics {
    private final double cpuLoadPercent;
    private final long ramTotalBytes;
    private final long ramUsedBytes;
    private final long ramFreeBytes;
    private final long diskTotalBytes;
    private final long diskUsedBytes;
    private final long diskFreeBytes;

    public SystemMetrics(
            double cpuLoadPercent,
            long ramTotalBytes, long ramUsedBytes, long ramFreeBytes,
            long diskTotalBytes, long diskUsedBytes, long diskFreeBytes
    ) {
        this.cpuLoadPercent = cpuLoadPercent;
        this.ramTotalBytes = ramTotalBytes;
        this.ramUsedBytes = ramUsedBytes;
        this.ramFreeBytes = ramFreeBytes;
        this.diskTotalBytes = diskTotalBytes;
        this.diskUsedBytes = diskUsedBytes;
        this.diskFreeBytes = diskFreeBytes;
    }

    public double getCpuLoadPercent() { return cpuLoadPercent; }
    public long getRamTotalBytes() { return ramTotalBytes; }
    public long getRamUsedBytes() { return ramUsedBytes; }
    public long getRamFreeBytes() { return ramFreeBytes; }
    public long getDiskTotalBytes() { return diskTotalBytes; }
    public long getDiskUsedBytes() { return diskUsedBytes; }
    public long getDiskFreeBytes() { return diskFreeBytes; }

    public double getRamUsagePercent() {
        return ratio(ramUsedBytes, ramTotalBytes);
    }

    public double getDiskUsagePercent() {
        return ratio(diskUsedBytes, diskTotalBytes);
    }

    private static double ratio(long used, long total) {
        if (total <= 0) return 0.0;
        return (used * 100.0) / total;
    }
}
