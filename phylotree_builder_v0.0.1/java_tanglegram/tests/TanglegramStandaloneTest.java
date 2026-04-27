package tanglegram;

import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TanglegramStandaloneTest {
    private TanglegramStandaloneTest() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        run("parsesEmptyArguments", TanglegramStandaloneTest::parsesEmptyArguments);
        run("parsesDirectoryArgument", TanglegramStandaloneTest::parsesDirectoryArgument);
        run("rejectsMissingDirectoryValue", TanglegramStandaloneTest::rejectsMissingDirectoryValue);
        run("disablesFlatlafNativeLibraryByDefault", TanglegramStandaloneTest::disablesFlatlafNativeLibraryByDefault);
        run("roundTripsUiPreferences", TanglegramStandaloneTest::roundTripsUiPreferences);
        run("resolvesStoredWindowSizes", TanglegramStandaloneTest::resolvesStoredWindowSizes);
        run("usesPreferenceBackedTanglegramDefaults", TanglegramStandaloneTest::usesPreferenceBackedTanglegramDefaults);
        run("resolvesMovedSampleTreesFromFallbackLayout", TanglegramStandaloneTest::resolvesMovedSampleTreesFromFallbackLayout);
        run("buildsFixedPairOrderForAllMethods", TanglegramStandaloneTest::buildsFixedPairOrderForAllMethods);
        run("buildsProteinStructurePairsWhenStructureTreeExists", TanglegramStandaloneTest::buildsProteinStructurePairsWhenStructureTreeExists);
        run("keepsFourMethodPairsWhenProteinStructureTreeIsMissing", TanglegramStandaloneTest::keepsFourMethodPairsWhenProteinStructureTreeIsMissing);
        run("loadsOnlyAvailablePairsWhenOneMethodIsMissing", TanglegramStandaloneTest::loadsOnlyAvailablePairsWhenOneMethodIsMissing);
        run("rendersPairPanelForResolvedTrees", TanglegramStandaloneTest::rendersPairPanelForResolvedTrees);
    }

    private static void parsesEmptyArguments() {
        LauncherOptions options = LauncherOptions.parse(new String[0]);
        assertTrue(options.treeSummaryDir().isEmpty(), "expected no startup directory");
        assertNull(options.startupError(), "unexpected startup error");
    }

    private static void parsesDirectoryArgument() {
        LauncherOptions options = LauncherOptions.parse(new String[] { "-dir", "sample/tree_summary" });
        assertTrue(options.treeSummaryDir().isPresent(), "expected startup directory");
        assertEquals(
                Paths.get("sample/tree_summary").toAbsolutePath().normalize(),
                options.treeSummaryDir().get(),
                "directory argument mismatch");
        assertNull(options.startupError(), "unexpected startup error");
    }

    private static void rejectsMissingDirectoryValue() {
        LauncherOptions options = LauncherOptions.parse(new String[] { "-dir" });
        assertTrue(options.treeSummaryDir().isEmpty(), "directory should be absent");
        assertTrue(options.startupError() != null && options.startupError().contains("-dir"),
                "expected missing -dir value error");
    }

    private static void disablesFlatlafNativeLibraryByDefault() {
        System.clearProperty(FlatLafBootstrap.USE_NATIVE_LIBRARY_PROPERTY);
        FlatLafBootstrap.prepareSystemProperties();
        assertEquals("false", System.getProperty(FlatLafBootstrap.USE_NATIVE_LIBRARY_PROPERTY),
                "expected FlatLaf native library to be disabled");
    }

    private static void roundTripsUiPreferences() {
        UiPreferenceStore.useTestNode("/egps-onebuilder/tests/tanglegram/preferences");
        UiPreferenceStore.clearNodeForTests();
        UiPreferenceStore.captureLookAndFeelDefaults();

        UiPreferences preferences = new UiPreferences("Dialog", 18, true, 21, true, UiLanguage.CHINESE);
        UiPreferenceStore.save(preferences);
        UiPreferences loaded = UiPreferenceStore.load();

        assertEquals("Dialog", loaded.uiFontFamily(), "unexpected font family");
        assertEquals(Integer.valueOf(18), Integer.valueOf(loaded.uiFontSize()), "unexpected font size");
        assertTrue(loaded.restoreLastWindowSize(), "expected restore window size to be enabled");
        assertEquals(Integer.valueOf(21), Integer.valueOf(loaded.defaultTanglegramLabelFontSize()),
                "unexpected default tanglegram label size");
        assertTrue(loaded.showWindowsOneBuilderWarning(), "expected Windows startup warning to be enabled");
        assertEquals(UiLanguage.CHINESE, loaded.uiLanguage(), "expected stored UI language");

        UiPreferenceStore.resetNodeForTests();
    }

    private static void resolvesStoredWindowSizes() {
        UiPreferenceStore.useTestNode("/egps-onebuilder/tests/tanglegram/window-size");
        UiPreferenceStore.clearNodeForTests();
        UiPreferenceStore.captureLookAndFeelDefaults();
        UiPreferenceStore.save(new UiPreferences("Dialog", 14, true, 12, true));
        UiPreferenceStore.saveWindowSize("tanglegram", new Dimension(1600, 1000));

        assertEquals(new Dimension(1600, 1000), UiPreferenceStore.resolveWindowSize("tanglegram", new Dimension(1400, 900)),
                "expected stored size to be used");

        UiPreferenceStore.save(new UiPreferences("Dialog", 14, false, 12, true));
        assertEquals(new Dimension(1400, 900), UiPreferenceStore.resolveWindowSize("tanglegram", new Dimension(1400, 900)),
                "expected fallback size when restore is disabled");

        UiPreferenceStore.resetNodeForTests();
    }

    private static void usesPreferenceBackedTanglegramDefaults() {
        UiPreferenceStore.useTestNode("/egps-onebuilder/tests/tanglegram/render-defaults");
        UiPreferenceStore.clearNodeForTests();
        UiPreferenceStore.captureLookAndFeelDefaults();
        UiPreferenceStore.save(new UiPreferences("Dialog", 14, true, 26, true));

        TanglegramRenderOptions defaults = TanglegramRenderOptions.defaults();
        assertEquals(Integer.valueOf(26), Integer.valueOf(defaults.labelFontSize()), "expected label size from preferences");

        UiPreferenceStore.resetNodeForTests();
    }

    private static void resolvesMovedSampleTreesFromFallbackLayout() throws Exception {
        Path movedOutput = copySampleOutput();

        TreeSummaryLoadResult result = TreeSummaryLoader.load(movedOutput.resolve("tree_summary"));

        assertEquals(4, result.resolvedTrees().size(), "expected all four trees");
        assertTrue(result.missingMethods().isEmpty(), "expected no missing methods");
        for (Map.Entry<TreeMethod, Path> entry : result.resolvedTrees().entrySet()) {
            assertTrue(entry.getValue().startsWith(movedOutput),
                    "expected fallback-resolved tree inside copied output for " + entry.getKey());
            assertTrue(Files.exists(entry.getValue()), "resolved tree should exist for " + entry.getKey());
        }
    }

    private static void buildsFixedPairOrderForAllMethods() throws Exception {
        Path movedOutput = copySampleOutput();

        TreeSummaryLoadResult result = TreeSummaryLoader.load(movedOutput.resolve("tree_summary"));
        List<TreePairSpec> pairs = result.availablePairs();

        assertEquals(6, pairs.size(), "expected six tree pairs");
        assertEquals("NJ-ML", pairs.get(0).tabName(), "pair 1 mismatch");
        assertEquals("NJ-BI", pairs.get(1).tabName(), "pair 2 mismatch");
        assertEquals("NJ-MP", pairs.get(2).tabName(), "pair 3 mismatch");
        assertEquals("ML-BI", pairs.get(3).tabName(), "pair 4 mismatch");
        assertEquals("ML-MP", pairs.get(4).tabName(), "pair 5 mismatch");
        assertEquals("BI-MP", pairs.get(5).tabName(), "pair 6 mismatch");
    }

    private static void buildsProteinStructurePairsWhenStructureTreeExists() throws Exception {
        Path movedOutput = copySampleOutput();
        Path structureTree = movedOutput.resolve("protein_structure").resolve("structure_tree.nwk");
        Files.createDirectories(structureTree.getParent());
        Files.writeString(structureTree, "(seq1:0.1,seq2:0.1,seq3:0.2);\n");
        Files.writeString(
                movedOutput.resolve("tree_summary").resolve("tree_meta_data.tsv"),
                "Protein_structure\t" + structureTree + "\n",
                java.nio.file.StandardOpenOption.APPEND);

        TreeSummaryLoadResult result = TreeSummaryLoader.load(movedOutput.resolve("tree_summary"));
        List<TreePairSpec> pairs = result.availablePairs();

        assertEquals(5, result.resolvedTrees().size(), "expected all five trees");
        assertEquals(10, pairs.size(), "expected ten tree pairs");
        assertEquals("NJ-PS", pairs.get(3).tabName(), "pair with structure tree should follow NJ-MP");
        assertEquals("MP-PS", pairs.get(9).tabName(), "last pair should compare MP and structure tree");
    }

    private static void keepsFourMethodPairsWhenProteinStructureTreeIsMissing() throws Exception {
        Path movedOutput = copySampleOutput();

        TreeSummaryLoadResult result = TreeSummaryLoader.loadRunResult(movedOutput);

        assertEquals(4, result.resolvedTrees().size(), "expected four resolved trees without structure tree");
        assertEquals(6, result.availablePairs().size(), "expected original six pairs without structure tree");
        assertTrue(!result.missingMethods().contains(TreeMethod.PROTEIN_STRUCTURE),
                "missing optional structure tree should not be a required missing method");
    }

    private static void loadsOnlyAvailablePairsWhenOneMethodIsMissing() throws Exception {
        Path movedOutput = copySampleOutput();
        deleteRecursively(movedOutput.resolve("maximum_likelihood"));

        TreeSummaryLoadResult result = TreeSummaryLoader.load(movedOutput.resolve("tree_summary"));

        assertEquals(3, result.resolvedTrees().size(), "expected three resolved trees");
        assertEquals(Arrays.asList(TreeMethod.ML_IQTREE), result.missingMethods(), "expected ML to be missing");
        assertEquals(
                Arrays.asList("NJ-BI", "NJ-MP", "BI-MP"),
                result.availablePairs().stream().map(TreePairSpec::tabName).collect(Collectors.toList()),
                "unexpected remaining pair order");
    }

    private static void rendersPairPanelForResolvedTrees() throws Exception {
        Path movedOutput = copySampleOutput();
        TreeSummaryLoadResult result = TreeSummaryLoader.load(movedOutput.resolve("tree_summary"));
        TreePairSpec firstPair = result.availablePairs().get(0);

        ResizableTanglegramView view = new ResizableTanglegramView(firstPair, new TanglegramPanelFactory());
        view.setSize(900, 700);
        view.renderNowForTest(new Dimension(900, 700));

        assertTrue(view.getComponentCount() > 0 && view.getComponent(0) != null, "expected rendered viewport content");
    }

    private static Path copySampleOutput() throws Exception {
        Path repoRoot = findRepoRoot();
        Path source = repoRoot.resolve("test1");
        if (!Files.isDirectory(source)) {
            throw new IllegalStateException("Missing sample output directory: " + source);
        }

        Path tempDir = Files.createTempDirectory("tanglegram-sample-");
        copyRecursively(source, tempDir);
        return tempDir;
    }

    private static void copyRecursively(Path source, Path target) throws Exception {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.collect(Collectors.toList())) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursively(Path target) throws Exception {
        try (Stream<Path> paths = Files.walk(target)) {
            for (Path path : paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .collect(Collectors.toList())) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void run(String name, ThrowingRunnable test) throws Exception {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (Throwable throwable) {
            System.err.println("FAIL " + name + ": " + throwable.getMessage());
            throw throwable;
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    private static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message + " value=[" + value + "]");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static Path findRepoRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        Path cursor = current;
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve("test1"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Could not locate repo root from " + current);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
