package tanglegram;

import evoltree.struct.EvolNode;
import evoltree.struct.TreeDecoder;
import evoltree.struct.util.EvolTreeOperator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class TreeDataLoader {
    private TreeDataLoader() {
    }

    static ImportedTreeSpec loadImportedTree(ImportedTreeSpec importedTree) throws Exception {
        return importedTree.withRoot(loadTree(importedTree.path()));
    }

    static EvolNode loadTree(Path treeFile) throws Exception {
        TreeDecoder decoder = new TreeDecoder();
        return decoder.decode(new String(Files.readAllBytes(treeFile), StandardCharsets.UTF_8).trim());
    }

    static EvolNode copyTree(EvolNode root) {
        if (root == null) {
            return null;
        }
        return EvolTreeOperator.copyTheTree(root);
    }
}
