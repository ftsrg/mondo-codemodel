package hu.bme.mit.codemodel.jamoppdiscoverer.benchmark;

import hu.bme.mit.codemodel.jamoppdiscoverer.utils.RelativePath;

import java.io.File;

public class DiscoveryBenchmarkResults {

    protected final String relativePath;

    protected final Long fileSize;

    protected Long fileDiscoveryTime = 0L;

    protected Long loadDependencyTime = 0L;

    protected Long dependencyResolutionTime = 0L;

    protected Long xmiExportTime = 0L;

    protected Long ntExportTime = 0L;

    protected Long ttlConvertTime = 0L;

    protected Long sumSize = 0L;

    protected Long dependencyCount = 0L;

    // -----------------------------------------------------------------------------------------------------------------

    protected Type activeType = null;

    // -----------------------------------------------------------------------------------------------------------------

    public DiscoveryBenchmarkResults(File file) {
        this.relativePath = RelativePath.of(file.getAbsolutePath());
        this.fileSize = file.length();
    }

    public void start(Type type) {
        switch (type) {
            case FILE_DISCOVERY:
                fileDiscoveryTime = System.currentTimeMillis();
                break;

            case LOAD_DEPENDENCY:
                loadDependencyTime = System.currentTimeMillis();
                break;

            case DEPENDENCY_RESOLUTION:
                dependencyResolutionTime = System.currentTimeMillis();
                break;

            case XMI_EXPORT:
                xmiExportTime = System.currentTimeMillis();
                break;

            case NT_EXPORT:
                ntExportTime = System.currentTimeMillis();
                break;

            case TTL_CONVERT:
                ttlConvertTime = System.currentTimeMillis();
                break;
        }

        activeType = type;
    }

    public long tick() {
        if (activeType == null) {
            return -1L;
        }

        switch (activeType) {
            case FILE_DISCOVERY:
                fileDiscoveryTime = System.currentTimeMillis() - fileDiscoveryTime;
                return fileDiscoveryTime;

            case LOAD_DEPENDENCY:
                loadDependencyTime = System.currentTimeMillis() - loadDependencyTime;
                return loadDependencyTime;

            case DEPENDENCY_RESOLUTION:
                dependencyResolutionTime = System.currentTimeMillis() - dependencyResolutionTime;
                return dependencyResolutionTime;

            case XMI_EXPORT:
                xmiExportTime = System.currentTimeMillis() - xmiExportTime;
                return xmiExportTime;

            case NT_EXPORT:
                ntExportTime = System.currentTimeMillis() - ntExportTime;
                return ntExportTime;

            case TTL_CONVERT:
                ttlConvertTime = System.currentTimeMillis() - ttlConvertTime;
                return ttlConvertTime;
        }

        return -1L;
    }

    public void addDependencySize(Long size) {
        this.sumSize += size;
        this.dependencyCount++;
    }

    public String toReadableString() {
        return "DiscoveryBenchmarkResults{" +
                "\n relativePath='" + relativePath + '\'' +
                "\n, fileSize=" + fileSize +
                "\n, fileDiscoveryTime=" + fileDiscoveryTime +
                "\n, loadDependencyTime=" + loadDependencyTime +
                "\n, dependencyResolutionTime=" + dependencyResolutionTime +
                "\n, xmiExportTime=" + xmiExportTime +
                "\n, ntExportTime=" + ntExportTime +
                "\n, ttlConvertTime=" + ttlConvertTime +
                "\n, sumSize=" + sumSize +
                "\n, dependencyCount=" + dependencyCount +
                "\n}";
    }

    public String header() {
        return "relativePath, fileSize, fileDiscoveryTime, loadDependencyTime, dependencyResolutionTime, xmiExportTime, ntExportTime, ttlConvertTime, sumSize, dependencyCount";
    }

    @Override
    public String toString() {
        return relativePath + ", " +
                fileSize + ", " +
                fileDiscoveryTime + ", " +
                loadDependencyTime + ", " +
                dependencyResolutionTime + ", " +
                xmiExportTime + ", " +
                ntExportTime + ", " +
                ttlConvertTime + ", " +
                sumSize + ", " +
                dependencyCount;
    }

    public enum Type {
        FILE_DISCOVERY,
        LOAD_DEPENDENCY,
        DEPENDENCY_RESOLUTION,
        XMI_EXPORT,
        NT_EXPORT,
        TTL_CONVERT
    }
}
