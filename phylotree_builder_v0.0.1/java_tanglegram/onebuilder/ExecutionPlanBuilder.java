package onebuilder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ExecutionPlanBuilder {
    private final Path scriptDir;

    public ExecutionPlanBuilder(Path scriptDir) {
        this.scriptDir = scriptDir;
    }

    public ExecutionPlan build(RunRequest request, Path configPath) {
        Path pipelineOutputDir = request.outputDirectory().resolve(request.outputPrefix()).normalize();
        Path effectiveInputFile = request.runAlignmentFirst()
                ? alignedOutputPath(request.inputFile())
                : request.inputFile().normalize();

        List<String> alignCommand = null;
        if (request.runAlignmentFirst()) {
            alignCommand = new ArrayList<>();
            alignCommand.add("/bin/zsh");
            alignCommand.add(scriptDir.resolve("s1_quick_align.zsh").toString());
            alignCommand.add("--config");
            alignCommand.add(configPath.toString());
            alignCommand.add(request.inputFile().toString());
        }

        List<String> buildCommand = new ArrayList<>();
        buildCommand.add("/bin/zsh");
        buildCommand.add(scriptDir.resolve(request.inputType().buildScriptName()).toString());
        buildCommand.add("--config");
        buildCommand.add(configPath.toString());
        buildCommand.add(effectiveInputFile.toString());
        buildCommand.add(pipelineOutputDir.toString());
        return new ExecutionPlan(alignCommand, buildCommand, pipelineOutputDir, effectiveInputFile);
    }

    static Path alignedOutputPath(Path inputFile) {
        String fileName = inputFile.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String alignedFileName;
        if (extensionIndex > 0) {
            alignedFileName = fileName.substring(0, extensionIndex) + ".aligned" + fileName.substring(extensionIndex);
        } else {
            alignedFileName = fileName + ".aligned";
        }
        Path parent = inputFile.getParent();
        if (parent == null) {
            return java.nio.file.Paths.get(alignedFileName);
        }
        return parent.resolve(alignedFileName).normalize();
    }
}
