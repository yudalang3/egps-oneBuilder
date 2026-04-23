package onebuilder;

import java.nio.file.Path;
import tanglegram.UiLanguage;

public final class RunRequest {
    private final InputType inputType;
    private final Path inputFile;
    private final Path outputDirectory;
    private final String outputPrefix;
    private final boolean exportConfigFile;
    private final boolean overwriteExistingOutput;
    private final boolean runAlignmentFirst;
    private final AlignmentOptions alignOptions;
    private final PipelineRuntimeConfig runtimeConfig;
    private final UiLanguage language;

    private RunRequest(Builder builder) {
        this.inputType = builder.inputType;
        this.inputFile = builder.inputFile;
        this.outputDirectory = builder.outputDirectory;
        this.outputPrefix = builder.outputPrefix;
        this.exportConfigFile = builder.exportConfigFile;
        this.overwriteExistingOutput = builder.overwriteExistingOutput;
        this.runAlignmentFirst = builder.runAlignmentFirst;
        this.alignOptions = builder.alignOptions;
        this.runtimeConfig = builder.runtimeConfig;
        this.language = builder.language == null ? UiLanguage.ENGLISH : builder.language;
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

    public boolean exportConfigFile() {
        return exportConfigFile;
    }

    public boolean overwriteExistingOutput() {
        return overwriteExistingOutput;
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

    public UiLanguage language() {
        return language;
    }

    public Path exportConfigPath() {
        return ConfigExportPaths.defaultJsonPath(outputDirectory, outputPrefix);
    }

    public Path pipelineOutputDir() {
        return outputDirectory.resolve(outputPrefix).toAbsolutePath().normalize();
    }

    public RunRequest withOverwriteExistingOutput(boolean overwriteExistingOutput) {
        return RunRequest.builder()
                .inputType(inputType)
                .inputFile(inputFile)
                .outputDirectory(outputDirectory)
                .outputPrefix(outputPrefix)
                .exportConfigFile(exportConfigFile)
                .overwriteExistingOutput(overwriteExistingOutput)
                .runAlignmentFirst(runAlignmentFirst)
                .alignOptions(alignOptions)
                .runtimeConfig(runtimeConfig)
                .language(language)
                .build();
    }

    public static final class Builder {
        private InputType inputType;
        private Path inputFile;
        private Path outputDirectory;
        private String outputPrefix;
        private boolean exportConfigFile;
        private boolean overwriteExistingOutput;
        private boolean runAlignmentFirst;
        private AlignmentOptions alignOptions = AlignmentOptions.defaults();
        private PipelineRuntimeConfig runtimeConfig;
        private UiLanguage language = UiLanguage.ENGLISH;

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

        public Builder exportConfigFile(boolean exportConfigFile) {
            this.exportConfigFile = exportConfigFile;
            return this;
        }

        public Builder overwriteExistingOutput(boolean overwriteExistingOutput) {
            this.overwriteExistingOutput = overwriteExistingOutput;
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

        public Builder language(UiLanguage language) {
            this.language = language;
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
