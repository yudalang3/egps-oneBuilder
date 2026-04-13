package onebuilder;

import java.nio.file.Path;

final class ConfigExportPaths {
    private ConfigExportPaths() {
    }

    static Path defaultJsonPath(Path outputDirectory, String outputPrefix) {
        return outputDirectory.resolve(outputPrefix + ".onebuilder.json").toAbsolutePath().normalize();
    }
}
