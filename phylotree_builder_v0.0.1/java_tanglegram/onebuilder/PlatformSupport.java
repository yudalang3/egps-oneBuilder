package onebuilder;

public enum PlatformSupport {
    LINUX(true),
    WINDOWS(false),
    OTHER(false);

    private final boolean pipelineExecution;

    PlatformSupport(boolean pipelineExecution) {
        this.pipelineExecution = pipelineExecution;
    }

    public boolean supportsPipelineExecution() {
        return pipelineExecution;
    }

    public static PlatformSupport current() {
        return detect(System.getProperty("os.name", ""));
    }

    static PlatformSupport detect(String osName) {
        String normalized = osName == null ? "" : osName.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("win")) {
            return WINDOWS;
        }
        if (normalized.contains("linux")) {
            return LINUX;
        }
        return OTHER;
    }
}
