package tanglegram;

import evoltree.struct.EvolNode;
import evoltree.struct.TreeDecoder;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

public final class TanglegramStandaloneTest {
    private static final String SUITE_PREFERENCE_NODE = "/egps-onebuilder/tests/tanglegram/suite";

    private TanglegramStandaloneTest() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
        UiPreferenceStore.clearNodeForTests();
        try {
            run("parsesEmptyArguments", TanglegramStandaloneTest::parsesEmptyArguments);
            run("parsesDirectoryArgument", TanglegramStandaloneTest::parsesDirectoryArgument);
            run("rejectsMissingDirectoryValue", TanglegramStandaloneTest::rejectsMissingDirectoryValue);
            run("disablesFlatlafNativeLibraryByDefault", TanglegramStandaloneTest::disablesFlatlafNativeLibraryByDefault);
            run("loadsEgpsWindowIcons", TanglegramStandaloneTest::loadsEgpsWindowIcons);
            run("roundTripsUiPreferences", TanglegramStandaloneTest::roundTripsUiPreferences);
            run("resolvesStoredWindowSizes", TanglegramStandaloneTest::resolvesStoredWindowSizes);
            run("usesPreferenceBackedTanglegramDefaults", TanglegramStandaloneTest::usesPreferenceBackedTanglegramDefaults);
            run("configuresWelcomeImportActions", TanglegramStandaloneTest::configuresWelcomeImportActions);
            run("defaultsTreeLeafArrangementRules", TanglegramStandaloneTest::defaultsTreeLeafArrangementRules);
            run("leavesArrangementDisabledUntilApplied", TanglegramStandaloneTest::leavesArrangementDisabledUntilApplied);
            run("arrangesByCladeSizeUp", TanglegramStandaloneTest::arrangesByCladeSizeUp);
            run("arrangesByCladeSizeDown", TanglegramStandaloneTest::arrangesByCladeSizeDown);
            run("arrangesByLeafNameStringUp", TanglegramStandaloneTest::arrangesByLeafNameStringUp);
            run("arrangesByLeafNameStringDown", TanglegramStandaloneTest::arrangesByLeafNameStringDown);
            run("leafNameStringListComparatorChecksAllEntries", TanglegramStandaloneTest::leafNameStringListComparatorChecksAllEntries);
            run("arrangesByBranchLengthUp", TanglegramStandaloneTest::arrangesByBranchLengthUp);
            run("arrangesByBranchLengthDown", TanglegramStandaloneTest::arrangesByBranchLengthDown);
            run("ruleOrderControlsArrangementPriority", TanglegramStandaloneTest::ruleOrderControlsArrangementPriority);
            run("preservesTopologyLeafNamesAndBranchLengths", TanglegramStandaloneTest::preservesTopologyLeafNamesAndBranchLengths);
            run("preservesParentLinksAfterArrangement", TanglegramStandaloneTest::preservesParentLinksAfterArrangement);
            run("keepsStableOrderWhenRulesTie", TanglegramStandaloneTest::keepsStableOrderWhenRulesTie);
            run("arrangesRecursivelyFromRootToLeaves", TanglegramStandaloneTest::arrangesRecursivelyFromRootToLeaves);
            run("renderingArrangementDoesNotMutatePreparedTrees", TanglegramStandaloneTest::renderingArrangementDoesNotMutatePreparedTrees);
            run("supportsTreeLeafArrangementControls", TanglegramStandaloneTest::supportsTreeLeafArrangementControls);
            run("configuresStandaloneResultActions", TanglegramStandaloneTest::configuresStandaloneResultActions);
            run("resolvesMovedSampleTreesFromFallbackLayout", TanglegramStandaloneTest::resolvesMovedSampleTreesFromFallbackLayout);
            run("buildsFixedPairOrderForAllMethods", TanglegramStandaloneTest::buildsFixedPairOrderForAllMethods);
            run("buildsProteinStructurePairsWhenStructureTreeExists", TanglegramStandaloneTest::buildsProteinStructurePairsWhenStructureTreeExists);
            run("keepsFourMethodPairsWhenProteinStructureTreeIsMissing", TanglegramStandaloneTest::keepsFourMethodPairsWhenProteinStructureTreeIsMissing);
            run("loadsOnlyAvailablePairsWhenOneMethodIsMissing", TanglegramStandaloneTest::loadsOnlyAvailablePairsWhenOneMethodIsMissing);
            run("rendersPairPanelForResolvedTrees", TanglegramStandaloneTest::rendersPairPanelForResolvedTrees);
            run("supportsStandaloneVisualPropertiesControls", TanglegramStandaloneTest::supportsStandaloneVisualPropertiesControls);
            run("supportsThreeDAlignmentControls", TanglegramStandaloneTest::supportsThreeDAlignmentControls);
            run("defaultsThreeDAlignmentRootAnnotation", TanglegramStandaloneTest::defaultsThreeDAlignmentRootAnnotation);
            run("roundTripsConsistencyAnnotationTsv", TanglegramStandaloneTest::roundTripsConsistencyAnnotationTsv);
            run("reordersThreeDTreeCards", TanglegramStandaloneTest::reordersThreeDTreeCards);
        } finally {
            UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
            UiPreferenceStore.clearNodeForTests();
            UiPreferenceStore.resetNodeForTests();
        }
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

    private static void loadsEgpsWindowIcons() {
        assertEquals(Integer.valueOf(3), Integer.valueOf(WindowIconSupport.loadIconImagesForTest().size()),
                "expected 16/32/72 eGPS window icons to be available on the classpath");
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

        UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
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

        UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
    }

    private static void usesPreferenceBackedTanglegramDefaults() {
        UiPreferenceStore.useTestNode("/egps-onebuilder/tests/tanglegram/render-defaults");
        UiPreferenceStore.clearNodeForTests();
        UiPreferenceStore.captureLookAndFeelDefaults();
        UiPreferenceStore.save(new UiPreferences("Dialog", 14, true, 26, true));

        TanglegramRenderOptions defaults = TanglegramRenderOptions.defaults();
        assertEquals(Integer.valueOf(26), Integer.valueOf(defaults.labelFontSize()), "expected label size from preferences");

        UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
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

    private static void configuresWelcomeImportActions() throws Exception {
        TanglegramWelcomePanel panel = createWelcomePanel();
        List<JButton> buttons = findButtons(panel);

        JButton loadRunningResultButton = findButton(buttons, "Load Running Result");
        JButton loadConfigButton = findButton(buttons, "Load Config File");
        JButton exportConfigButton = findButton(buttons, "Export Config File");

        assertTrue(loadRunningResultButton.getToolTipText().contains("oneBuilder result folder"),
                "expected running result tooltip to describe oneBuilder import");
        assertEquals(loadConfigButton.getToolTipText(), exportConfigButton.getToolTipText(),
                "load/export config file tooltips should match");
        assertNull(findButtonOrNull(buttons, "Browse Selected..."),
                "Browse Selected button should be removed from the import panel");
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

    private static void supportsStandaloneVisualPropertiesControls() throws Exception {
        Path movedOutput = copySampleOutput();
        TreeSummaryLoadResult result = TreeSummaryLoader.load(movedOutput.resolve("tree_summary"));
        TreePairSpec firstPair = result.availablePairs().get(0);
        TanglegramRenderOptions current = TanglegramRenderOptions.defaults();
        TanglegramRenderOptions updated = new TanglegramRenderOptions(
                18,
                current.labelFontFamily(),
                java.awt.Font.BOLD,
                false,
                4,
                2,
                220,
                1.8f,
                10.0f,
                7.0f,
                true);
        ResizableTanglegramView view = new ResizableTanglegramView(firstPair, new TanglegramPanelFactory(updated), updated);
        view.setSize(900, 700);
        view.renderNowForTest(new Dimension(900, 700));

        assertTrue(view.getComponentCount() > 0 && view.getComponent(0) != null,
                "expected custom visual properties rendering content");
        assertTrue(!updated.showLeafLabels(), "expected updated leaf label visibility");
        assertEquals(Integer.valueOf(220), Integer.valueOf(updated.connectorGap()),
                "expected updated connector gap");
    }

    private static void supportsThreeDAlignmentControls() throws Exception {
        List<ImportedTreeSpec> trees = sampleImportedTrees();
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(trees);
        List<JButton> buttons = findButtons(view);

        JButton treeOrderButton = findButton(buttons, "Tree order");
        JButton annotationButton = findButton(buttons, "Consistency annotation");

        assertEquals(
                "Reorder the trees in this 3D alignment view without changing the imported data or tree files.",
                treeOrderButton.getToolTipText(),
                "unexpected tree order tooltip");
        assertEquals(
                "Connect clades or clusters that contain exactly the same leaf set across the aligned trees using translucent Sankey ribbons.",
                annotationButton.getToolTipText(),
                "unexpected consistency annotation tooltip");
        assertTrue(view.getExportComponent() != view, "3D alignment export should exclude bottom control buttons");
    }

    private static void defaultsThreeDAlignmentRootAnnotation() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTrees());

        assertEquals(Arrays.asList("Tree A", "Tree B"), view.displayedTreeLabelsForTest(),
                "unexpected initial 3D tree order");
        assertEquals(Integer.valueOf(1), Integer.valueOf(view.consistencyAnnotationsForTest().size()),
                "expected default root consistency annotation");
        ConsistencyAnnotation annotation = view.consistencyAnnotationsForTest().get(0);
        assertEquals(Arrays.asList("Dog", "Cow", "Frog"), annotation.leafNames(),
                "default root annotation should use all leaves from the first tree");
        assertEquals("#4F8CFFA0", annotation.colorText(), "unexpected default root annotation color");
        assertEquals(Double.valueOf(5.0d), Double.valueOf(annotation.ribbonWidth()),
                "default root annotation should use a thinner ribbon width");
    }

    private static void roundTripsConsistencyAnnotationTsv() throws Exception {
        Path tempFile = Files.createTempFile("consistency-annotation-", ".tsv");
        List<ConsistencyAnnotation> annotations = List.of(
                new ConsistencyAnnotation(Arrays.asList("Dog", "Cow", "Frog"), new Color(255, 162, 52, 162), 3.5d));

        ConsistencyAnnotationIO.writeTsv(tempFile, annotations);
        List<ConsistencyAnnotation> loaded = ConsistencyAnnotationIO.readTsv(tempFile);

        assertEquals(Integer.valueOf(1), Integer.valueOf(loaded.size()), "expected one annotation from TSV");
        assertEquals(Arrays.asList("Dog", "Cow", "Frog"), loaded.get(0).leafNames(),
                "unexpected loaded clade leaf names");
        assertEquals("#FFA234A2", loaded.get(0).colorText(), "unexpected loaded RGBA color");
        assertEquals(Double.valueOf(3.5d), Double.valueOf(loaded.get(0).ribbonWidth()),
                "unexpected loaded ribbon width");

        ConsistencyAnnotation legacyTwoColumn = ConsistencyAnnotationIO.parseRow("Dog,Cow,Frog", "#FFA234A2", 1);
        assertEquals(Double.valueOf(5.0d), Double.valueOf(legacyTwoColumn.ribbonWidth()),
                "two-column TSV rows should use the default thinner ribbon width");
    }

    private static void reordersThreeDTreeCards() throws Exception {
        ThreeDTreeOrderControlPanel controlPanel = new ThreeDTreeOrderControlPanel(sampleImportedTrees());

        assertEquals(Arrays.asList("Tree A", "Tree B"), controlPanel.treeLabelsForTest(),
                "unexpected initial tree card order");
        controlPanel.moveSelectedDownForTest();
        assertEquals(Arrays.asList("Tree B", "Tree A"), controlPanel.treeLabelsForTest(),
                "move-down should reorder tree cards");
        controlPanel.moveSelectedUpForTest();
        assertEquals(Arrays.asList("Tree A", "Tree B"), controlPanel.treeLabelsForTest(),
                "move-up should reorder tree cards");
    }

    private static void defaultsTreeLeafArrangementRules() {
        TreeLeafArrangementOptions defaults = TreeLeafArrangementOptions.defaults();

        assertTrue(defaults.enabled(), "expected dialog defaults to be enabled when applied");
        assertEquals(
                Arrays.asList(
                        TreeLeafArrangementRule.CLADE_SIZE,
                        TreeLeafArrangementRule.LEAF_NAME_STRING,
                        TreeLeafArrangementRule.BRANCH_LENGTH),
                defaults.ruleOrder(),
                "unexpected default tree leaf arrangement rules");
        assertEquals(TreeLeafArrangementDirection.UP, defaults.direction(), "unexpected default arrangement direction");
        assertTrue(!TreeLeafArrangementOptions.disabled().enabled(), "initial result view arrangement should be disabled");
    }

    private static void leavesArrangementDisabledUntilApplied() throws Exception {
        EvolNode root = decodeTree("((C:0.1,D:0.1,E:0.1):0.3,(A:0.1,B:0.1):0.2);");

        TreeLeafArrangementEngine.arrange(root, TreeLeafArrangementOptions.disabled());

        assertEquals(Arrays.asList("C", "D", "E", "A", "B"), leafOrder(root),
                "disabled arrangement should not change display order");
    }

    private static void arrangesByCladeSizeUp() throws Exception {
        EvolNode root = decodeTree("((C:0.1,D:0.1,E:0.1):0.3,(A:0.1,B:0.1):0.2);");

        TreeLeafArrangementEngine.arrange(root, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.CLADE_SIZE),
                TreeLeafArrangementDirection.UP));

        assertEquals(Arrays.asList("A", "B", "C", "D", "E"), leafOrder(root),
                "UP clade-size arrangement should draw smaller clades first");
    }

    private static void arrangesByCladeSizeDown() throws Exception {
        EvolNode root = decodeTree("((A:0.1,B:0.1):0.2,(C:0.1,D:0.1,E:0.1):0.3);");

        TreeLeafArrangementEngine.arrange(root, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.CLADE_SIZE),
                TreeLeafArrangementDirection.DOWN));

        assertEquals(Arrays.asList("E", "D", "C", "B", "A"), leafOrder(root),
                "DOWN clade-size arrangement should draw larger clades and later leaf-name ties first");
    }

    private static void arrangesByLeafNameStringUp() throws Exception {
        EvolNode root = decodeTree("((C:0.1,D:0.1):0.2,(A:0.1,B:0.1):0.2);");

        TreeLeafArrangementEngine.arrange(root, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.LEAF_NAME_STRING),
                TreeLeafArrangementDirection.UP));

        assertEquals(Arrays.asList("A", "B", "C", "D"), leafOrder(root),
                "UP leaf-name arrangement should draw earlier strings first");
    }

    private static void arrangesByLeafNameStringDown() throws Exception {
        EvolNode root = decodeTree("((A:0.1,B:0.1):0.2,(C:0.1,D:0.1):0.2);");

        TreeLeafArrangementEngine.arrange(root, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.LEAF_NAME_STRING),
                TreeLeafArrangementDirection.DOWN));

        assertEquals(Arrays.asList("D", "C", "B", "A"), leafOrder(root),
                "DOWN leaf-name arrangement should draw later strings first at every node");
    }

    private static void leafNameStringListComparatorChecksAllEntries() {
        assertTrue(TreeLeafArrangementEngine.compareLeafNameListsForTest(
                Arrays.asList("A", "Z"),
                Arrays.asList("A", "B")) > 0,
                "leaf-name list comparison should continue past equal leading names");
        assertTrue(TreeLeafArrangementEngine.compareLeafNameListsForTest(
                Arrays.asList("A", "B"),
                Arrays.asList("A", "B", "C")) < 0,
                "shorter equal-prefix leaf-name list should sort first");
    }

    private static void arrangesByBranchLengthUp() throws Exception {
        EvolNode root = decodeTree("((A:0.1,B:0.1):0.8,(C:0.1,D:0.1):0.2);");

        TreeLeafArrangementEngine.arrange(root, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.BRANCH_LENGTH),
                TreeLeafArrangementDirection.UP));

        assertEquals(Arrays.asList("C", "D", "A", "B"), leafOrder(root),
                "UP branch-length arrangement should draw shorter branches first");
    }

    private static void arrangesByBranchLengthDown() throws Exception {
        EvolNode root = decodeTree("((C:0.1,D:0.1):0.2,(A:0.1,B:0.1):0.8);");

        TreeLeafArrangementEngine.arrange(root, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.BRANCH_LENGTH),
                TreeLeafArrangementDirection.DOWN));

        assertEquals(Arrays.asList("B", "A", "D", "C"), leafOrder(root),
                "DOWN branch-length arrangement should draw longer branches and later leaf-name ties first");
    }

    private static void ruleOrderControlsArrangementPriority() throws Exception {
        EvolNode cladeFirstRoot = decodeTree("((Z:0.1):0.1,(A:0.1,B:0.1):0.9);");
        EvolNode leafNameFirstRoot = decodeTree("((Z:0.1):0.1,(A:0.1,B:0.1):0.9);");

        TreeLeafArrangementEngine.arrange(cladeFirstRoot, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.CLADE_SIZE, TreeLeafArrangementRule.LEAF_NAME_STRING),
                TreeLeafArrangementDirection.UP));
        TreeLeafArrangementEngine.arrange(leafNameFirstRoot, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.LEAF_NAME_STRING, TreeLeafArrangementRule.CLADE_SIZE),
                TreeLeafArrangementDirection.UP));

        assertEquals(Arrays.asList("Z", "A", "B"), leafOrder(cladeFirstRoot),
                "clade-size first should keep the one-leaf clade first");
        assertEquals(Arrays.asList("A", "B", "Z"), leafOrder(leafNameFirstRoot),
                "leaf-name first should override clade-size when rule order changes");
    }

    private static void preservesTopologyLeafNamesAndBranchLengths() throws Exception {
        EvolNode root = decodeTree("((D:0.4,C:0.3):0.8,(B:0.2,A:0.1):0.7);");
        List<String> beforeTopology = cladeSignatures(root);
        Map<String, Double> beforeLengths = branchLengthsByClade(root);
        List<String> beforeLeafNames = sortedLeafNames(root);

        TreeLeafArrangementEngine.arrange(root, TreeLeafArrangementOptions.defaults());

        assertEquals(beforeTopology, cladeSignatures(root), "arrangement should preserve topology clades");
        assertEquals(beforeLengths, branchLengthsByClade(root), "arrangement should preserve branch lengths");
        assertEquals(beforeLeafNames, sortedLeafNames(root), "arrangement should preserve leaf names");
    }

    private static void preservesParentLinksAfterArrangement() throws Exception {
        EvolNode root = decodeTree("(((D:0.1,C:0.1):0.2,(B:0.1,A:0.1):0.2):0.3,(F:0.1,E:0.1):0.2);");

        TreeLeafArrangementEngine.arrange(root, TreeLeafArrangementOptions.defaults());

        assertParentLinks(root);
    }

    private static void keepsStableOrderWhenRulesTie() throws Exception {
        EvolNode root = decodeTree("((A:0.1,B:0.1):0.2,(A:0.1,B:0.1):0.2);");

        TreeLeafArrangementEngine.arrange(root, TreeLeafArrangementOptions.defaults());

        assertEquals(Arrays.asList("A", "B", "A", "B"), leafOrder(root),
                "stable sort should keep original order when all arrangement rules tie");
    }

    private static void arrangesRecursivelyFromRootToLeaves() throws Exception {
        EvolNode root = decodeTree("(((D:0.1,C:0.1):0.2,(B:0.1,A:0.1):0.2):0.3,(F:0.1,E:0.1):0.2);");

        TreeLeafArrangementEngine.arrange(root, new TreeLeafArrangementOptions(
                true,
                Arrays.asList(TreeLeafArrangementRule.LEAF_NAME_STRING),
                TreeLeafArrangementDirection.UP));

        assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F"), leafOrder(root),
                "arrangement should recurse into every internal node");
    }

    private static void renderingArrangementDoesNotMutatePreparedTrees() throws Exception {
        Path tempDir = Files.createTempDirectory("tanglegram-arrangement-render-");
        Path leftTree = tempDir.resolve("left.nwk");
        Path rightTree = tempDir.resolve("right.nwk");
        Files.writeString(leftTree, "((C:0.1,D:0.1):0.2,(A:0.1,B:0.1):0.2);\n");
        Files.writeString(rightTree, "((D:0.1,C:0.1):0.2,(B:0.1,A:0.1):0.2);\n");

        TanglegramPanelFactory factory = new TanglegramPanelFactory(
                TanglegramRenderOptions.defaults(),
                TreeLeafArrangementOptions.defaults());
        TanglegramPanelFactory.PreparedPair preparedPair = factory.preparePair(new TreePairSpec("left", "right", leftTree, rightTree));
        List<String> originalLeftOrder = leafOrder(preparedPair.leftTree());
        List<String> originalRightOrder = leafOrder(preparedPair.rightTree());

        JPanel panel = factory.createPanel(preparedPair, new Dimension(900, 700));
        panel.setSize(900, 700);
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_ARGB);
        panel.paint(image.createGraphics());

        assertEquals(originalLeftOrder, leafOrder(preparedPair.leftTree()),
                "render-time arrangement should not mutate prepared left tree");
        assertEquals(originalRightOrder, leafOrder(preparedPair.rightTree()),
                "render-time arrangement should not mutate prepared right tree");
        deleteRecursively(tempDir);
    }

    private static void supportsTreeLeafArrangementControls() {
        TreeLeafArrangementControlPanel controlPanel = new TreeLeafArrangementControlPanel(TreeLeafArrangementOptions.defaults());

        assertEquals(
                Arrays.asList(
                        TreeLeafArrangementRule.CLADE_SIZE,
                        TreeLeafArrangementRule.LEAF_NAME_STRING,
                        TreeLeafArrangementRule.BRANCH_LENGTH),
                controlPanel.ruleOrderForTest(),
                "unexpected dialog default rule order");
        assertEquals(
                Arrays.asList("Clade size", "Leaf name string", "Branch length"),
                controlPanel.cardTitlesForTest(),
                "unexpected board card titles");
        assertTrue(controlPanel.detailTextForTest().contains("Clade size"),
                "default selected card should describe clade size");

        controlPanel.selectRuleForTest(TreeLeafArrangementRule.LEAF_NAME_STRING);
        assertTrue(controlPanel.detailTextForTest().contains("cached"),
                "leaf-name detail should mention cached leaf-name lists");
        assertTrue(controlPanel.detailTextForTest().contains("item-by-item"),
                "leaf-name detail should clarify full-list comparison");

        controlPanel.moveRule(2, 0);
        assertEquals(
                Arrays.asList(
                        TreeLeafArrangementRule.BRANCH_LENGTH,
                        TreeLeafArrangementRule.CLADE_SIZE,
                        TreeLeafArrangementRule.LEAF_NAME_STRING),
                controlPanel.ruleOrderForTest(),
                "drag model should reorder rules");
        controlPanel.moveSelectedDownForTest();
        assertEquals(
                Arrays.asList(
                        TreeLeafArrangementRule.CLADE_SIZE,
                        TreeLeafArrangementRule.BRANCH_LENGTH,
                        TreeLeafArrangementRule.LEAF_NAME_STRING),
                controlPanel.ruleOrderForTest(),
                "move-down fallback should reorder the selected card");
        controlPanel.moveSelectedUpForTest();
        assertEquals(
                Arrays.asList(
                        TreeLeafArrangementRule.BRANCH_LENGTH,
                        TreeLeafArrangementRule.CLADE_SIZE,
                        TreeLeafArrangementRule.LEAF_NAME_STRING),
                controlPanel.ruleOrderForTest(),
                "move-up fallback should reorder the selected card");
        controlPanel.setDirectionForTest(TreeLeafArrangementDirection.DOWN);
        assertEquals(TreeLeafArrangementDirection.DOWN, controlPanel.toOptions().direction(),
                "dialog control should expose selected direction");
    }

    private static void configuresStandaloneResultActions() throws Exception {
        JButton arrangementButton = new JButton("Tree leaf arrangement");
        JButton visualButton = new JButton("Visual properties");
        JButton threeDButton = new JButton("3D Tree Alignment");
        List<String> tooltips = TanglegramResultTabPanel.standaloneActionTooltipsForTest();
        arrangementButton.setToolTipText(tooltips.get(0));
        visualButton.setToolTipText(tooltips.get(1));
        threeDButton.setToolTipText(tooltips.get(2));

        JPanel actionPanel = TanglegramResultTabPanel.createStandaloneActionPanelForTest(
                arrangementButton,
                visualButton,
                threeDButton);
        List<JButton> buttons = findButtons(actionPanel);

        JButton foundArrangementButton = findButton(buttons, "Tree leaf arrangement");
        JButton foundVisualButton = findButton(buttons, "Visual properties");
        JButton foundThreeDButton = findButton(buttons, "3D Tree Alignment");

        assertEquals(TanglegramResultTabPanel.standaloneActionLabelsForTest(),
                Arrays.asList(foundArrangementButton.getText(), foundVisualButton.getText(), foundThreeDButton.getText()),
                "unexpected standalone action labels");
        assertEquals(tooltips,
                Arrays.asList(foundArrangementButton.getToolTipText(), foundVisualButton.getToolTipText(), foundThreeDButton.getToolTipText()),
                "unexpected standalone action tooltips");
        assertTrue(findSeparators(actionPanel).size() >= 1, "expected separator before 3D action group");
    }

    private static Path copySampleOutput() throws Exception {
        Path source = findSampleOutput();
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

    private static TanglegramWelcomePanel createWelcomePanel() throws Exception {
        AtomicReference<TanglegramWelcomePanel> panelRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panelRef.set(new TanglegramWelcomePanel(session -> {
        })));
        return panelRef.get();
    }

    private static List<JButton> findButtons(Component component) {
        List<JButton> buttons = new ArrayList<>();
        collectButtons(component, buttons);
        return buttons;
    }

    private static void collectButtons(Component component, List<JButton> buttons) {
        if (component instanceof JButton button) {
            buttons.add(button);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectButtons(child, buttons);
            }
        }
    }

    private static List<JSeparator> findSeparators(Component component) {
        List<JSeparator> separators = new ArrayList<>();
        collectSeparators(component, separators);
        return separators;
    }

    private static void collectSeparators(Component component, List<JSeparator> separators) {
        if (component instanceof JSeparator separator) {
            separators.add(separator);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectSeparators(child, separators);
            }
        }
    }

    private static JButton findButton(List<JButton> buttons, String text) {
        JButton button = findButtonOrNull(buttons, text);
        if (button == null) {
            throw new AssertionError("Missing button: " + text);
        }
        return button;
    }

    private static JButton findButtonOrNull(List<JButton> buttons, String text) {
        for (JButton button : buttons) {
            if (text.equals(button.getText())) {
                return button;
            }
        }
        return null;
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
            if (Files.isDirectory(cursor.resolve("test1"))
                    || Files.isDirectory(cursor.resolve("old_archive").resolve("test1"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Could not locate repo root from " + current);
    }

    private static Path findSampleOutput() {
        Path repoRoot = findRepoRoot();
        Path currentLayout = repoRoot.resolve("test1");
        if (Files.isDirectory(currentLayout)) {
            return currentLayout;
        }
        return repoRoot.resolve("old_archive").resolve("test1");
    }

    private static EvolNode decodeTree(String newick) throws Exception {
        return new TreeDecoder().decode(newick);
    }

    private static List<ImportedTreeSpec> sampleImportedTrees() throws Exception {
        return Arrays.asList(
                new ImportedTreeSpec(Path.of("tree-a.nwk"), "Tree A", decodeTree("((Dog:1,Cow:1):1,Frog:1);")),
                new ImportedTreeSpec(Path.of("tree-b.nwk"), "Tree B", decodeTree("((Dog:1,Cow:1):1,Frog:1);")));
    }

    private static List<String> leafOrder(EvolNode root) {
        List<String> leaves = new ArrayList<>();
        collectLeafOrder(root, leaves);
        return leaves;
    }

    private static void collectLeafOrder(EvolNode node, List<String> leaves) {
        if (node.getChildCount() == 0) {
            leaves.add(node.getName());
            return;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            collectLeafOrder(node.getChildAt(index), leaves);
        }
    }

    private static void assertParentLinks(EvolNode node) {
        for (int index = 0; index < node.getChildCount(); index++) {
            EvolNode child = node.getChildAt(index);
            assertTrue(child.getParent() == node, "arrangement should preserve child parent links");
            assertParentLinks(child);
        }
    }

    private static List<String> sortedLeafNames(EvolNode root) {
        List<String> leaves = leafOrder(root);
        leaves.sort(String::compareTo);
        return leaves;
    }

    private static List<String> cladeSignatures(EvolNode root) {
        List<String> signatures = new ArrayList<>();
        collectCladeSignatures(root, signatures);
        signatures.sort(String::compareTo);
        return signatures;
    }

    private static List<String> collectCladeSignatures(EvolNode node, List<String> signatures) {
        if (node.getChildCount() == 0) {
            return new ArrayList<>(List.of(node.getName()));
        }
        List<String> leaves = new ArrayList<>();
        for (int index = 0; index < node.getChildCount(); index++) {
            leaves.addAll(collectCladeSignatures(node.getChildAt(index), signatures));
        }
        leaves.sort(String::compareTo);
        signatures.add(String.join("|", leaves));
        return leaves;
    }

    private static Map<String, Double> branchLengthsByClade(EvolNode root) {
        java.util.LinkedHashMap<String, Double> lengths = new java.util.LinkedHashMap<>();
        collectBranchLengthsByClade(root, lengths);
        return lengths;
    }

    private static List<String> collectBranchLengthsByClade(EvolNode node, Map<String, Double> lengths) {
        if (node.getChildCount() == 0) {
            lengths.put(node.getName(), Double.valueOf(node.getLength()));
            return new ArrayList<>(List.of(node.getName()));
        }
        List<String> leaves = new ArrayList<>();
        for (int index = 0; index < node.getChildCount(); index++) {
            leaves.addAll(collectBranchLengthsByClade(node.getChildAt(index), lengths));
        }
        leaves.sort(String::compareTo);
        lengths.put(String.join("|", leaves), Double.valueOf(node.getLength()));
        return leaves;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
