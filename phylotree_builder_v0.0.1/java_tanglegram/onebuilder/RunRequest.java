package onebuilder;

import java.nio.file.Path;

public final class RunRequest {
    private final InputType inputType;
    private final Path inputFile;
    private final Path outputDirectory;
    private final String outputPrefix;
    private final boolean runAlignmentFirst;
    private final AlignmentOptions alignOptions;
    private final PipelineRuntimeConfig runtimeConfig;

    private RunRequest(Builder builder) {
        this.inputType = builder.inputType;
        this.inputFile = builder.inputFile;
        this.outputDirectory = builder.outputDirectory;
        this.outputPrefix = builder.outputPrefix;
        this.runAlignmentFirst = builder.runAlignmentFirst;
        this.alignOptions = builder.alignOptions;
        this.runtimeConfig = builder.runtimeConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public InputType inputType() {
        return inputType;
    }

    public Path inputFile() {
        return inputFile;
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public String outputPrefix() {
        return outputPrefix;
    }

    public boolean runAlignmentFirst() {
        return runAlignmentFirst;
    }

    public AlignmentOptions alignOptions() {
        return alignOptions;
    }

    public PipelineRuntimeConfig runtimeConfig() {
        return runtimeConfig;
    }

    public static final class Builder {
        private InputType inputType;
        private Path inputFile;
        private Path outputDirectory;
        private String outputPrefix;
        private boolean runAlignmentFirst;
        private AlignmentOptions alignOptions = AlignmentOptions.defaults();
        private PipelineRuntimeConfig runtimeConfig;

        public Builder inputType(InputType inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder inputFile(Path inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public Builder outputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public Builder outputPrefix(String outputPrefix) {
            this.outputPrefix = outputPrefix;
            return this;
        }

        public Builder runAlignmentFirst(boolean runAlignmentFirst) {
            this.runAlignmentFirst = runAlignmentFirst;
            return this;
        }

        public Builder alignOptions(AlignmentOptions alignOptions) {
            this.alignOptions = alignOptions;
            return this;
        }

        public Builder runtimeConfig(PipelineRuntimeConfig runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
            return this;
        }

        public RunRequest build() {
            if (inputType == null || inputFile == null || outputDirectory == null || outputPrefix == null
                    || outputPrefix.trim().isEmpty() || runtimeConfig == null) {
                throw new IllegalStateException("RunRequest is missing required fields");
            }
            return new RunRequest(this);
        }
    }
}
