package onebuilder;

import evoltree.phylogeny.DefaultPhyNode;
import evoltree.phylogeny.PhyloTreeEncoderDecoder;
import evoltree.struct.EvolNode;
import evoltree.struct.util.EvolNodeUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TreePostprocessCommand {
    private TreePostprocessCommand() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            CommandOptions options = CommandOptions.parse(args);
            DefaultPhyNode root = readTree(options.inputFile);
            if (options.clampNegativeBranchLengths) {
                clampNegativeBranchLengths(root);
            }
            if (options.setAllBranchLengths != null) {
                setAllBranchLengths(root, options.setAllBranchLengths.doubleValue());
            }
            if (options.renameMapFile != null) {
                renameLeaves(root, readRenameMap(options.renameMapFile));
            }
            if (options.ladderizeDirection != null) {
                EvolNodeUtil.initializeSize(root);
                applyLadderization(root, options);
            }
            writeTree(root, options.outputFile);
            return 0;
        } catch (Exception exception) {
            System.err.println("Failed to postprocess tree: " + exception.getMessage());
            return 1;
        }
    }

    private static DefaultPhyNode readTree(Path inputFile) throws Exception {
        if (!Files.isRegularFile(inputFile)) {
            throw new IllegalArgumentException("Input tree does not exist: " + inputFile);
        }
        String newick = Files.readString(inputFile, StandardCharsets.UTF_8).trim();
        if (newick.isEmpty()) {
            throw new IllegalArgumentException("Input tree is empty: " + inputFile);
        }
        return new PhyloTreeEncoderDecoder().decode(newick);
    }

    private static void writeTree(DefaultPhyNode root, Path outputFile) throws Exception {
        String output = new PhyloTreeEncoderDecoder().encode(root).trim();
        if (!output.endsWith(";")) {
            output += ";";
        }
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, output + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static void clampNegativeBranchLengths(DefaultPhyNode root) {
        EvolNodeUtil.recursiveIterateTreeIF(root, node -> {
            if (node.getLength() < 0.0d) {
                node.setLength(0.0d);
            }
        });
    }

    private static void setAllBranchLengths(DefaultPhyNode root, double branchLength) {
        EvolNodeUtil.recursiveIterateTreeIF(root, node -> node.setLength(branchLength));
    }

    private static void renameLeaves(DefaultPhyNode root, Map<String, String> renameMap) {
        List<DefaultPhyNode> leaves = EvolNodeUtil.getLeaves(root);
        for (DefaultPhyNode leaf : leaves) {
            String currentName = leaf.getName();
            String normalizedName = currentName == null ? "" : currentName.trim();
            String replacement = renameMap.get(normalizedName);
            if (replacement != null) {
                leaf.setName(replacement);
            } else if (!normalizedName.equals(currentName)) {
                leaf.setName(normalizedName);
            }
        }
    }

    private static Map<String, String> readRenameMap(Path renameMapFile) throws Exception {
        if (!Files.isRegularFile(renameMapFile)) {
            throw new IllegalArgumentException("Rename map does not exist: " + renameMapFile);
        }
        Map<String, String> renameMap = new LinkedHashMap<>();
        for (String rawLine : Files.readAllLines(renameMapFile, StandardCharsets.UTF_8)) {
            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }
            String[] parts = rawLine.split("\t", 2);
            if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                throw new IllegalArgumentException("Rename map line must be <oldName><tab><newName>: " + rawLine);
            }
            renameMap.put(parts[0].trim(), parts[1].trim());
        }
        return renameMap;
    }

    private static void applyLadderization(DefaultPhyNode root, CommandOptions options) {
        boolean upwards = options.ladderizeDirection == LadderizeDirection.UP;
        if (options.sortByCladeSize && options.sortByBranchLength) {
            EvolNodeUtil.ladderizeNodeAccording2sizeAndLength(root, upwards);
            return;
        }
        if (options.sortByCladeSize) {
            EvolNodeUtil.ladderizeNodeAccording2size(root, upwards);
            return;
        }
        EvolNodeUtil.ladderizeNode(root, upwards);
    }

    private static final class CommandOptions {
        private final Path inputFile;
        private final Path outputFile;
        private final Path renameMapFile;
        private final boolean clampNegativeBranchLengths;
        private final Double setAllBranchLengths;
        private final LadderizeDirection ladderizeDirection;
        private final boolean sortByCladeSize;
        private final boolean sortByBranchLength;

        private CommandOptions(
                Path inputFile,
                Path outputFile,
                Path renameMapFile,
                boolean clampNegativeBranchLengths,
                Double setAllBranchLengths,
                LadderizeDirection ladderizeDirection,
                boolean sortByCladeSize,
                boolean sortByBranchLength) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.renameMapFile = renameMapFile;
            this.clampNegativeBranchLengths = clampNegativeBranchLengths;
            this.setAllBranchLengths = setAllBranchLengths;
            this.ladderizeDirection = ladderizeDirection;
            this.sortByCladeSize = sortByCladeSize;
            this.sortByBranchLength = sortByBranchLength;
        }

        private static CommandOptions parse(String[] args) {
            Path inputFile = null;
            Path outputFile = null;
            Path renameMapFile = null;
            boolean clampNegativeBranchLengths = false;
            Double setAllBranchLengths = null;
            LadderizeDirection ladderizeDirection = null;
            boolean sortByCladeSize = false;
            boolean sortByBranchLength = false;
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                if ("--input".equals(arg)) {
                    inputFile = Paths.get(requireValue(args, ++index, arg)).toAbsolutePath().normalize();
                } else if ("--output".equals(arg)) {
                    outputFile = Paths.get(requireValue(args, ++index, arg)).toAbsolutePath().normalize();
                } else if ("--rename-map".equals(arg)) {
                    renameMapFile = Paths.get(requireValue(args, ++index, arg)).toAbsolutePath().normalize();
                } else if ("--clamp-negative-branch-lengths".equals(arg)) {
                    clampNegativeBranchLengths = true;
                } else if ("--set-all-branch-lengths".equals(arg)) {
                    setAllBranchLengths = Double.valueOf(requireValue(args, ++index, arg));
                } else if ("--ladderize-direction".equals(arg)) {
                    ladderizeDirection = LadderizeDirection.fromJsonValue(requireValue(args, ++index, arg));
                } else if ("--sort-by-clade-size".equals(arg)) {
                    sortByCladeSize = true;
                } else if ("--sort-by-branch-length".equals(arg)) {
                    sortByBranchLength = true;
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (inputFile == null || outputFile == null) {
                throw new IllegalArgumentException(
                        "Usage: onebuilder.TreePostprocessCommand --input input.nwk --output output.nwk [--rename-map names.tsv] [--clamp-negative-branch-lengths] [--set-all-branch-lengths 1] [--ladderize-direction UP|DOWN] [--sort-by-clade-size] [--sort-by-branch-length]");
            }
            return new CommandOptions(
                    inputFile,
                    outputFile,
                    renameMapFile,
                    clampNegativeBranchLengths,
                    setAllBranchLengths,
                    ladderizeDirection,
                    sortByCladeSize,
                    sortByBranchLength);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
