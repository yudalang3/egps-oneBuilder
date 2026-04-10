package tanglegram;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

final class LauncherOptions {
    private final Path treeSummaryDir;
    private final String startupError;

    private LauncherOptions(Path treeSummaryDir, String startupError) {
        this.treeSummaryDir = treeSummaryDir;
        this.startupError = startupError;
    }

    static LauncherOptions parse(String[] args) {
        Path parsedTreeSummaryDir = null;

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if ("-dir".equals(arg)) {
                if (index + 1 >= args.length) {
                    return new LauncherOptions(null, "Missing value for -dir");
                }
                if (parsedTreeSummaryDir != null) {
                    return new LauncherOptions(null, "Duplicate -dir argument");
                }
                parsedTreeSummaryDir = Paths.get(args[++index]).toAbsolutePath().normalize();
            } else {
                return new LauncherOptions(null, "Unknown argument: " + arg);
            }
        }

        return new LauncherOptions(parsedTreeSummaryDir, null);
    }

    Optional<Path> treeSummaryDir() {
        return Optional.ofNullable(treeSummaryDir);
    }

    String startupError() {
        return startupError;
    }
}
