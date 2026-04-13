package onebuilder;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class ExecutionPlan {
    private final List<String> alignCommand;
    private final List<String> buildCommand;
    private final Path pipelineOutputDir;
    private final Path effectiveInputFile;

    public ExecutionPlan(List<String> alignCommand, List<String> buildCommand, Path pipelineOutputDir, Path effectiveInputFile) {
        this.alignCommand = alignCommand;
        this.buildCommand = buildCommand;
        this.pipelineOutputDir = pipelineOutputDir;
        this.effectiveInputFile = effectiveInputFile;
    }

    public Optional<List<String>> alignCommand() {
        return Optional.ofNullable(alignCommand);
    }

    public List<String> buildCommand() {
        return buildCommand;
    }

    public Path pipelineOutputDir() {
        return pipelineOutputDir;
    }

    public Path effectiveInputFile() {
        return effectiveInputFile;
    }
}
