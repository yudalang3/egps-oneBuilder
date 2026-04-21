package tanglegram;

import evoltree.struct.EvolNode;
import java.nio.file.Path;

record ImportedTreeSpec(Path path, String label, EvolNode root) {
    ImportedTreeSpec(Path path, String label) {
        this(path, label, null);
    }

    ImportedTreeSpec withRoot(EvolNode root) {
        return new ImportedTreeSpec(path, label, root);
    }
}
