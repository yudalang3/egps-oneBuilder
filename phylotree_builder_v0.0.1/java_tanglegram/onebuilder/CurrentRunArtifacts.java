package onebuilder;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CurrentRunArtifacts {
    private CurrentRunArtifacts() {
    }

    public static Path resolveTreeSummaryDir(Path outputDirectory) {
        return outputDirectory.resolve("tree_summary").normalize();
    }

    public static boolean hasRenderableTanglegram(Path outputDirectory) {
        Path treeSummaryDir = resolveTreeSummaryDir(outputDirectory);
        return Files.isDirectory(treeSummaryDir) && Files.isRegularFile(treeSummaryDir.resolve("tree_meta_data.tsv"));
    }
}
