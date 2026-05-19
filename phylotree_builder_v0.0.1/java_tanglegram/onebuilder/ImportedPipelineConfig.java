package onebuilder;

import java.nio.file.Path;

final class ImportedPipelineConfig {
    private final InputType inputType;
    private final Path inputFile;
    private final Path outputDirectory;
    private final String outputPrefix;
    private final boolean runAlignmentFirst;
    private final AlignmentOptions alignOptions;
    private final TrimAlignmentConfig trimAlignmentConfig;
    private final PipelineRuntimeConfig runtimeConfig;

    ImportedPipelineConfig(
            InputType inputType,
            Path inputFile,
            Path outputDirectory,
            String outputPrefix,
            boolean runAlignmentFirst,
            AlignmentOptions alignOptions,
            TrimAlignmentConfig trimAlignmentConfig,
            PipelineRuntimeConfig runtimeConfig) {
        this.inputType = inputType;
        this.inputFile = inputFile;
        this.outputDirectory = outputDirectory;
        this.outputPrefix = outputPrefix;
        this.runAlignmentFirst = runAlignmentFirst;
        this.alignOptions = alignOptions == null ? AlignmentOptions.defaults() : alignOptions;
        this.trimAlignmentConfig = trimAlignmentConfig == null ? TrimAlignmentConfig.defaults() : trimAlignmentConfig;
        this.runtimeConfig = runtimeConfig;
    }

    InputType inputType() {
        return inputType;
    }

    Path inputFile() {
        return inputFile;
    }

    Path outputDirectory() {
        return outputDirectory;
    }

    String outputPrefix() {
        return outputPrefix;
    }

    boolean runAlignmentFirst() {
        return runAlignmentFirst;
    }

    AlignmentOptions alignOptions() {
        return alignOptions;
    }

    TrimAlignmentConfig trimAlignmentConfig() {
        return trimAlignmentConfig;
    }

    PipelineRuntimeConfig runtimeConfig() {
        return runtimeConfig;
    }
}
