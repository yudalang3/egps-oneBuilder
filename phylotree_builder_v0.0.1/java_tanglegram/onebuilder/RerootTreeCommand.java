package onebuilder;

import evoltree.phylogeny.DefaultPhyNode;
import evoltree.phylogeny.PhyloTreeEncoderDecoder;
import evoltree.struct.EvolNode;
import evoltree.struct.util.EvolTreeOperator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RerootTreeCommand {
    private RerootTreeCommand() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            CommandOptions options = CommandOptions.parse(args);
            if (options.method != RerootMethod.ROOT_AT_MIDDLE_POINT) {
                throw new IllegalArgumentException("RerootTreeCommand supports only root-at-middle-point.");
            }
            rerootAtMiddlePoint(options.inputFile, options.outputFile);
            return 0;
        } catch (Exception exception) {
            System.err.println("Failed to reroot tree: " + exception.getMessage());
            return 1;
        }
    }

    private static void rerootAtMiddlePoint(Path inputFile, Path outputFile) throws Exception {
        if (!Files.isRegularFile(inputFile)) {
            throw new IllegalArgumentException("Input tree does not exist: " + inputFile);
        }
        String newick = normalizeInputNewick(Files.readString(inputFile, StandardCharsets.UTF_8).trim());
        if (newick.isEmpty()) {
            throw new IllegalArgumentException("Input tree is empty: " + inputFile);
        }

        PhyloTreeEncoderDecoder codec = new PhyloTreeEncoderDecoder();
        DefaultPhyNode root = codec.decode(newick);
        int removedBlankLeafCount = TreeLeafSanitizer.removeBlankZeroLengthLeaves(root);
        if (removedBlankLeafCount > 0) {
            System.err.println("WARNING: removed " + removedBlankLeafCount
                    + " blank zero-length terminal leaf artifact(s)");
        }
        TreeLeafSanitizer.validateNamedLeaves(root);
        EvolNode rerooted = EvolTreeOperator.rootAtMidPoint(root);
        DefaultPhyNode outputRoot = root;
        if (!(rerooted instanceof DefaultPhyNode)) {
            String returnedType = rerooted == null ? "null" : rerooted.getClass().getSimpleName();
            System.err.println("WARNING: midpoint rooting left tree unchanged because eGPS returned "
                    + returnedType);
        } else {
            outputRoot = (DefaultPhyNode) rerooted;
        }
        removedBlankLeafCount = TreeLeafSanitizer.removeBlankZeroLengthLeaves(outputRoot);
        if (removedBlankLeafCount > 0) {
            System.err.println("WARNING: removed " + removedBlankLeafCount
                    + " blank zero-length terminal leaf artifact(s) after rerooting");
        }
        TreeLeafSanitizer.validateNamedLeaves(outputRoot);
        String output = codec.encode(outputRoot).trim();
        if (!output.endsWith(";")) {
            output += ";";
        }
        writeOutput(outputFile, output);
    }

    private static void writeOutput(Path outputFile, String output) throws Exception {
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, output + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static String normalizeInputNewick(String newick) {
        int firstTerminator = newick.indexOf(';');
        String firstTree = firstTerminator >= 0 ? newick.substring(0, firstTerminator + 1) : newick;
        return firstTree.replaceAll("\\[[^\\[\\]]*\\]", "").trim();
    }

    private static final class CommandOptions {
        private final RerootMethod method;
        private final Path inputFile;
        private final Path outputFile;

        private CommandOptions(RerootMethod method, Path inputFile, Path outputFile) {
            this.method = method;
            this.inputFile = inputFile;
            this.outputFile = outputFile;
        }

        private static CommandOptions parse(String[] args) {
            RerootMethod method = null;
            Path inputFile = null;
            Path outputFile = null;
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                if ("--method".equals(arg)) {
                    method = parseMethod(requireValue(args, ++index, arg));
                } else if ("--input".equals(arg)) {
                    inputFile = Paths.get(requireValue(args, ++index, arg)).toAbsolutePath().normalize();
                } else if ("--output".equals(arg)) {
                    outputFile = Paths.get(requireValue(args, ++index, arg)).toAbsolutePath().normalize();
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (method == null || inputFile == null || outputFile == null) {
                throw new IllegalArgumentException("Usage: onebuilder.RerootTreeCommand --method root-at-middle-point --input input.nwk --output output.nwk");
            }
            return new CommandOptions(method, inputFile, outputFile);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }

        private static RerootMethod parseMethod(String rawValue) {
            for (RerootMethod method : RerootMethod.values()) {
                if (method.jsonValue().equals(rawValue)) {
                    return method;
                }
            }
            throw new IllegalArgumentException("Unsupported reroot method: " + rawValue);
        }
    }
}
