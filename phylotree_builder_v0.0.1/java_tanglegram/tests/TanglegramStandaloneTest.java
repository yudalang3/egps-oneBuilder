package tanglegram;

import evoltree.struct.EvolNode;
import evoltree.struct.TreeDecoder;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
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
            run("rendersTanglegramWithUnnamedLeaves", TanglegramStandaloneTest::rendersTanglegramWithUnnamedLeaves);
            run("wrapsTanglegramCanvasInScrollableExportableView", TanglegramStandaloneTest::wrapsTanglegramCanvasInScrollableExportableView);
            run("supportsTanglegramViewportNavigationMenus", TanglegramStandaloneTest::supportsTanglegramViewportNavigationMenus);
            run("usesScreenCoordinatesForSmoothViewportDragging", TanglegramStandaloneTest::usesScreenCoordinatesForSmoothViewportDragging);
            run("reusesTanglegramLayoutAcrossRepaints", TanglegramStandaloneTest::reusesTanglegramLayoutAcrossRepaints);
            run("supportsStandaloneVisualPropertiesControls", TanglegramStandaloneTest::supportsStandaloneVisualPropertiesControls);
            run("preferenceFontFamilyNamesDoNotBlockWhenCold", TanglegramStandaloneTest::preferenceFontFamilyNamesDoNotBlockWhenCold);
            run("supportsThreeDAlignmentControls", TanglegramStandaloneTest::supportsThreeDAlignmentControls);
            run("supportsThreeDViewportNavigationMenus", TanglegramStandaloneTest::supportsThreeDViewportNavigationMenus);
            run("keepsViewportStableWhenOpeningContextMenuAfterZoom", TanglegramStandaloneTest::keepsViewportStableWhenOpeningContextMenuAfterZoom);
            run("providesCopyableThreeDNodeInformationWithLeafNames", TanglegramStandaloneTest::providesCopyableThreeDNodeInformationWithLeafNames);
            run("hidesStaleThreeDRenderAfterCanvasResize", TanglegramStandaloneTest::hidesStaleThreeDRenderAfterCanvasResize);
            run("centersThreeDLoadingMessageInVisibleViewport", TanglegramStandaloneTest::centersThreeDLoadingMessageInVisibleViewport);
            run("rendersThreeDAlignmentWithUnnamedLeaves", TanglegramStandaloneTest::rendersThreeDAlignmentWithUnnamedLeaves);
            run("defaultsThreeDAlignmentRootAnnotation", TanglegramStandaloneTest::defaultsThreeDAlignmentRootAnnotation);
            run("preparesThreeDAnnotationAnchorsBeforePainting", TanglegramStandaloneTest::preparesThreeDAnnotationAnchorsBeforePainting);
            run("preparesThreeDAnnotationsWhenSomeTreesLackClade", TanglegramStandaloneTest::preparesThreeDAnnotationsWhenSomeTreesLackClade);
            run("resolvesNamedInternalNodeConsistencyAnnotation", TanglegramStandaloneTest::resolvesNamedInternalNodeConsistencyAnnotation);
            run("quickLabelsAndCleansThreeDConsistencyAnnotations", TanglegramStandaloneTest::quickLabelsAndCleansThreeDConsistencyAnnotations);
            run("calculatesZeroTreeDifferenceForIdenticalTrees", TanglegramStandaloneTest::calculatesZeroTreeDifferenceForIdenticalTrees);
            run("calculatesZeroTopologyDifferenceForSameCladesWithDifferentChildOrder", TanglegramStandaloneTest::calculatesZeroTopologyDifferenceForSameCladesWithDifferentChildOrder);
            run("calculatesTopologyDifferenceForMissingReferenceClades", TanglegramStandaloneTest::calculatesTopologyDifferenceForMissingReferenceClades);
            run("calculatesMaximumTopologyDifferenceWhenReferenceCladesAreAbsent", TanglegramStandaloneTest::calculatesMaximumTopologyDifferenceWhenReferenceCladesAreAbsent);
            run("averagesTopologyDifferenceAcrossMultipleOtherTrees", TanglegramStandaloneTest::averagesTopologyDifferenceAcrossMultipleOtherTrees);
            run("averagesTopologyDifferenceAcrossMultipleReferenceClades", TanglegramStandaloneTest::averagesTopologyDifferenceAcrossMultipleReferenceClades);
            run("keepsTopologyDifferenceReferenceBased", TanglegramStandaloneTest::keepsTopologyDifferenceReferenceBased);
            run("returnsUnavailableMetricsForSingleTree", TanglegramStandaloneTest::returnsUnavailableMetricsForSingleTree);
            run("returnsUnavailableMetricsForReferenceTreeWithoutInternalClades", TanglegramStandaloneTest::returnsUnavailableMetricsForReferenceTreeWithoutInternalClades);
            run("calculatesBranchLengthDifferenceForMatchedClades", TanglegramStandaloneTest::calculatesBranchLengthDifferenceForMatchedClades);
            run("calculatesExpectedBranchLengthDifferenceForKnownLengths", TanglegramStandaloneTest::calculatesExpectedBranchLengthDifferenceForKnownLengths);
            run("averagesBranchLengthDifferenceAcrossMatchedClades", TanglegramStandaloneTest::averagesBranchLengthDifferenceAcrossMatchedClades);
            run("returnsUnavailableBranchLengthDifferenceWithoutMatchedClades", TanglegramStandaloneTest::returnsUnavailableBranchLengthDifferenceWithoutMatchedClades);
            run("boundsBranchLengthDifferenceForExtremeLengths", TanglegramStandaloneTest::boundsBranchLengthDifferenceForExtremeLengths);
            run("ignoresMissingCladesForBranchLengthDifference", TanglegramStandaloneTest::ignoresMissingCladesForBranchLengthDifference);
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

    private static void rendersTanglegramWithUnnamedLeaves() throws Exception {
        EvolNode leftTree = decodeTree("((A:0.1,B:0.1):0.2,C:0.3);");
        EvolNode rightTree = decodeTree("((A:0.1,B:0.1):0.2,C:0.3);");
        firstLeaf(leftTree).setName(null);
        firstLeaf(rightTree).setName(null);

        TanglegramPanelFactory factory = new TanglegramPanelFactory();
        JPanel panel = factory.createPanel(
                new TanglegramPanelFactory.PreparedPair(leftTree, rightTree),
                new Dimension(900, 700));
        panel.setSize(900, 700);
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_ARGB);

        panel.paint(image.createGraphics());

        assertTrue(countNearBlackPixels(image) > 0, "unnamed leaves should not prevent tanglegram rendering");
    }

    private static void wrapsTanglegramCanvasInScrollableExportableView() throws Exception {
        ResizableTanglegramView view = renderedTanglegramView();
        JScrollPane scrollPane = findRequiredScrollPane(view);
        JComponent exportComponent = invokeNoArg(view, "getExportComponent", JComponent.class);

        assertTrue(scrollPane.getViewport().getView() == exportComponent,
                "expected scroll pane viewport to hold the exportable drawing component");
        assertTrue(exportComponent != view && exportComponent != scrollPane,
                "expected export to exclude the outer view and scroll bars");
    }

    private static void supportsTanglegramViewportNavigationMenus() throws Exception {
        ResizableTanglegramView view = renderedTanglegramView();
        JScrollPane scrollPane = findRequiredScrollPane(view);
        JViewport viewport = scrollPane.getViewport();
        JComponent exportComponent = invokeNoArg(view, "getExportComponent", JComponent.class);
        Dimension beforeZoom = exportComponent.getPreferredSize();

        dispatchWheel(exportComponent, beforeZoom.width / 2, beforeZoom.height / 2, -1);
        Dimension afterZoom = exportComponent.getPreferredSize();
        assertTrue(afterZoom.width > beforeZoom.width && afterZoom.height > beforeZoom.height,
                "mouse wheel should enlarge the tanglegram drawing component");

        dispatchLeftDrag(exportComponent, new Point(220, 220), new Point(80, 70));
        Point viewPosition = viewport.getViewPosition();
        assertTrue(viewPosition.x > 0 || viewPosition.y > 0,
                "left-button drag should pan the viewport");

        invokeNoArg(view, "fitFrameForTest", Object.class);
        Dimension extent = viewport.getExtentSize();
        Dimension fitted = exportComponent.getPreferredSize();
        assertEquals(extent, fitted, "Fit frame should restore the drawing component to the viewport extent");

        Point nodePoint = invokeNoArg(view, "firstNodePointForTest", Point.class);
        JPopupMenu nodeMenu = invokeWithPoint(view, "popupMenuForTest", nodePoint, JPopupMenu.class);
        assertEquals(
                Arrays.asList("Display more information", "Zoom to see node"),
                menuItemLabels(nodeMenu),
                "unexpected node popup items");

        JPopupMenu blankMenu = invokeWithPoint(view, "popupMenuForTest", new Point(3, 3), JPopupMenu.class);
        assertEquals(
                Arrays.asList("Refresh (Fit frame)", "Zoom the area"),
                menuItemLabels(blankMenu),
                "unexpected blank-area popup items");
    }

    private static void usesScreenCoordinatesForSmoothViewportDragging() throws Exception {
        ResizableTanglegramView view = renderedTanglegramView();
        JScrollPane scrollPane = findRequiredScrollPane(view);
        JViewport viewport = scrollPane.getViewport();
        JComponent exportComponent = invokeNoArg(view, "getExportComponent", JComponent.class);
        dispatchWheel(exportComponent, 450, 350, -2);
        viewport.setViewPosition(new Point(0, 0));

        long now = System.currentTimeMillis();
        dispatchMouse(exportComponent, MouseEvent.MOUSE_PRESSED, now, 300, 300, 1000, 1000, MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        assertEquals(
                Integer.valueOf(Cursor.CROSSHAIR_CURSOR),
                Integer.valueOf(exportComponent.getCursor().getType()),
                "left-button drag should show the crosshair cursor");

        dispatchMouse(exportComponent, MouseEvent.MOUSE_DRAGGED, now + 1L, 200, 200, 900, 900, MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON);
        assertEquals(new Point(100, 100), viewport.getViewPosition(),
                "first drag event should pan by the screen delta");

        dispatchMouse(exportComponent, MouseEvent.MOUSE_DRAGGED, now + 2L, 300, 300, 880, 880, MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON);
        assertEquals(new Point(120, 120), viewport.getViewPosition(),
                "later drag events should ignore component-coordinate feedback from viewport movement");

        dispatchMouse(exportComponent, MouseEvent.MOUSE_RELEASED, now + 3L, 300, 300, 880, 880, 0, MouseEvent.BUTTON1);
        assertEquals(
                Integer.valueOf(Cursor.DEFAULT_CURSOR),
                Integer.valueOf(exportComponent.getCursor().getType()),
                "cursor should reset after left-button drag");
    }

    private static void reusesTanglegramLayoutAcrossRepaints() throws Exception {
        EvolNode leftTree = decodeTree("((C:0.1,D:0.1):0.2,(A:0.1,B:0.1):0.3);");
        EvolNode rightTree = decodeTree("((D:0.1,C:0.1):0.2,(B:0.1,A:0.1):0.3);");
        TanglegramPanelFactory factory = new TanglegramPanelFactory(
                TanglegramRenderOptions.defaults(),
                TreeLeafArrangementOptions.defaults());
        JPanel panel = factory.createPanel(
                new TanglegramPanelFactory.PreparedPair(leftTree, rightTree),
                new Dimension(900, 700));
        panel.setSize(900, 700);
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_ARGB);

        panel.paint(image.createGraphics());
        Object firstLayout = getPrivateField(panel, "cachedLayout", Object.class);
        panel.paint(image.createGraphics());
        Object secondLayout = getPrivateField(panel, "cachedLayout", Object.class);

        assertTrue(firstLayout == secondLayout,
                "tanglegram layout should be reused across repaint-only operations to keep viewport dragging smooth");
    }

    private static void preferenceFontFamilyNamesDoNotBlockWhenCold() throws Exception {
        setPrivateStaticField(TanglegramVisualPropertiesDialog.class, "cachedFontFamilyNames", null);

        String[] names = TanglegramVisualPropertiesDialog.availableFontFamilyNamesForDialog();

        assertTrue(names.length > 0, "expected a non-empty fallback font family list before background scan completes");
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
        JButton quickButton = findButton(buttons, "Quick label consistency");
        JButton cleanButton = findButton(buttons, "Clean all labels");

        assertEquals(
                "Reorder the trees in this 3D alignment view without changing the imported data or tree files.",
                treeOrderButton.getToolTipText(),
                "unexpected tree order tooltip");
        assertEquals(
                "Connect clades or clusters that contain exactly the same leaf set across the aligned trees using translucent Sankey ribbons.",
                annotationButton.getToolTipText(),
                "unexpected consistency annotation tooltip");
        assertEquals(
                "Automatically create up to 10 consistency labels from internal clades in the first tree, then connect exact matches across the ordered trees.",
                quickButton.getToolTipText(),
                "unexpected quick label tooltip");
        assertEquals(
                "Remove all consistency labels and hide all annotation ribbons, markers, and legend entries from this 3D Alignment view.",
                cleanButton.getToolTipText(),
                "unexpected clean labels tooltip");
        assertTrue(view.getExportComponent() != view, "3D alignment export should exclude bottom control buttons");
    }

    private static void supportsThreeDViewportNavigationMenus() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTrees());
        view.setSize(1100, 780);
        view.doLayout();
        JScrollPane scrollPane = findRequiredScrollPane(view);
        assertTrue(findButtons(view).size() >= 4, "expected bottom 3D controls to remain outside the scroll pane");
        assertTrue(scrollPane.getViewport().getView() == view.getExportComponent(),
                "3D scroll pane should wrap the exportable canvas");

        view.renderForCurrentSizeForTest();
        waitUntil(view::canExport, 3000, "3D alignment view did not finish rendering");
        JComponent canvas = view.getExportComponent();
        Dimension beforeZoom = canvas.getPreferredSize();
        Long sequenceBefore = invokeNoArg(view, "renderSequenceForTest", Long.class);

        dispatchWheel(canvas, beforeZoom.width / 2, beforeZoom.height / 2, -1);
        Dimension afterZoom = canvas.getPreferredSize();
        Long sequenceAfter = invokeNoArg(view, "renderSequenceForTest", Long.class);
        assertTrue(afterZoom.width > beforeZoom.width && afterZoom.height > beforeZoom.height,
                "mouse wheel should enlarge the 3D alignment canvas");
        assertTrue(sequenceAfter.longValue() > sequenceBefore.longValue(),
                "3D zoom should schedule an asynchronous rerender");

        Point nodePoint = invokeNoArg(view, "firstNodePointForTest", Point.class);
        String hitSummary = invokeWithPoint(view, "hitSummaryForTest", nodePoint, String.class);
        assertTrue(hitSummary.contains("3D Tree Alignment") && hitSummary.contains("Tree A"),
                "3D hit testing should identify the rendered layer and node");

        JPopupMenu nodeMenu = invokeWithPoint(view, "popupMenuForTest", nodePoint, JPopupMenu.class);
        assertEquals(
                Arrays.asList("Display more information", "Zoom to see node"),
                menuItemLabels(nodeMenu),
                "unexpected 3D node popup items");

        JPopupMenu blankMenu = invokeWithPoint(view, "popupMenuForTest", new Point(3, 3), JPopupMenu.class);
        assertEquals(
                Arrays.asList("Refresh (Fit frame)", "Zoom the area"),
                menuItemLabels(blankMenu),
                "unexpected 3D blank-area popup items");
    }

    private static void keepsViewportStableWhenOpeningContextMenuAfterZoom() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTrees());
        view.setSize(1100, 780);
        view.doLayout();
        view.renderForCurrentSizeForTest();
        waitUntil(view::canExport, 3000, "3D alignment view did not finish rendering before context-menu check");

        JScrollPane scrollPane = findRequiredScrollPane(view);
        JViewport viewport = scrollPane.getViewport();
        JComponent canvas = view.getExportComponent();
        dispatchWheel(canvas, 560, 360, -2);
        viewport.setViewPosition(new Point(140, 110));
        Dimension sizeBefore = canvas.getPreferredSize();
        Point viewPositionBefore = viewport.getViewPosition();

        JPopupMenu blankMenu = invokeWithPoint(view, "popupMenuForTest", new Point(560, 360), JPopupMenu.class);

        assertTrue(!blankMenu.isLightWeightPopupEnabled(),
                "context menu should use a heavyweight popup so it is not clipped or repainted as part of the zoomed canvas");
        assertEquals(sizeBefore, canvas.getPreferredSize(),
                "opening a context menu after zoom must not resize the 3D canvas");
        assertEquals(viewPositionBefore, viewport.getViewPosition(),
                "opening a context menu after zoom must not pan the viewport");
    }

    private static void providesCopyableThreeDNodeInformationWithLeafNames() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTrees());
        view.setSize(1100, 780);
        view.doLayout();
        view.renderForCurrentSizeForTest();
        waitUntil(view::canExport, 3000, "3D alignment view did not finish rendering before node-information check");

        Point nodePoint = invokeNoArg(view, "firstNodePointForTest", Point.class);
        String informationText = invokeWithPoint(view, "informationTextForTest", nodePoint, String.class);
        assertTrue(informationText.contains("Leaf names: Dog,Cow,Frog"),
                "root node information should include copyable descendant leaf names for consistency annotations");
        assertTrue(informationText.contains("Copy value for Consistency annotation: Dog,Cow,Frog"),
                "node information should expose the exact comma-separated consistency annotation value");
        assertTrue(informationText.contains("View type: 3D Tree Alignment"),
                "node information should retain the basic view metadata");
    }

    private static void hidesStaleThreeDRenderAfterCanvasResize() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTrees());
        view.setSize(1100, 780);
        view.doLayout();
        view.renderForCurrentSizeForTest();
        waitUntil(view::canExport, 3000, "3D alignment view did not finish rendering before stale-render check");

        JComponent canvas = view.getExportComponent();
        Dimension renderedSize = canvas.getSize();
        canvas.setPreferredSize(new Dimension(renderedSize.width + 240, renderedSize.height + 160));
        canvas.setSize(canvas.getPreferredSize());
        setPrivateBoolean(view, "loading", true);

        BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        canvas.paint(image.createGraphics());

        assertTrue(countNearBlackPixels(image) < 50,
                "resized 3D canvas should not paint stale black tree geometry while rerendering");
    }

    private static void centersThreeDLoadingMessageInVisibleViewport() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTrees());
        view.setSize(900, 640);
        view.doLayout();
        JScrollPane scrollPane = findRequiredScrollPane(view);
        JViewport viewport = scrollPane.getViewport();
        JComponent canvas = view.getExportComponent();
        scrollPane.setSize(900, 560);
        scrollPane.doLayout();
        canvas.setPreferredSize(new Dimension(1800, 1280));
        canvas.setSize(1800, 1280);
        canvas.revalidate();
        viewport.setViewPosition(new Point(700, 430));
        setPrivateBoolean(view, "loading", true);

        BufferedImage image = new BufferedImage(1800, 1280, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setClip(viewport.getViewRect());
        canvas.paint(graphics);
        graphics.dispose();

        assertTrue(countNonWhitePixels(image, viewport.getViewRect()) > 0,
                "3D loading message should be painted inside the currently visible scrolled viewport");
    }

    private static void rendersThreeDAlignmentWithUnnamedLeaves() throws Exception {
        EvolNode leftTree = decodeTree("((A:0.1,B:0.1):0.2,C:0.3);");
        EvolNode rightTree = decodeTree("((A:0.1,B:0.1):0.2,C:0.3);");
        firstLeaf(leftTree).setName(null);
        firstLeaf(rightTree).setName(null);
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(List.of(
                new ImportedTreeSpec(Path.of("tree-a.nwk"), "Tree A", leftTree),
                new ImportedTreeSpec(Path.of("tree-b.nwk"), "Tree B", rightTree)));
        view.setSize(1100, 780);
        view.doLayout();
        view.renderForCurrentSizeForTest();
        waitUntil(view::canExport, 3000, "3D alignment with unnamed leaves did not finish rendering");

        JComponent canvas = view.getExportComponent();
        Dimension canvasSize = canvas.getPreferredSize();
        canvas.setSize(canvasSize);
        BufferedImage image = new BufferedImage(canvasSize.width, canvasSize.height, BufferedImage.TYPE_INT_ARGB);
        canvas.paint(image.createGraphics());

        assertTrue(countNearBlackPixels(image) > 0, "unnamed leaves should not prevent 3D alignment rendering");
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

    private static void preparesThreeDAnnotationAnchorsBeforePainting() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTrees());
        view.setSize(1100, 780);
        view.doLayout();
        view.renderForCurrentSizeForTest();
        waitUntil(view::canExport, 3000, "3D alignment view did not finish rendering");

        assertEquals(Integer.valueOf(1), Integer.valueOf(view.preparedAnnotationCountForTest()),
                "expected annotation geometry to be prepared during rendering");
        assertEquals(Integer.valueOf(2), Integer.valueOf(view.preparedAnnotationAnchorCountForTest()),
                "expected one root annotation anchor for each rendered tree");
    }

    private static void preparesThreeDAnnotationsWhenSomeTreesLackClade() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTreesWithMissingQuickClade());
        view.setSize(1100, 780);
        view.doLayout();
        view.quickLabelConsistencyForTest();
        view.renderForCurrentSizeForTest();
        waitUntil(view::canExport, 3000, "3D alignment view did not finish rendering with missing clade annotations");

        assertEquals(Integer.valueOf(1), Integer.valueOf(view.preparedAnnotationCountForTest()),
                "expected quick annotation to be prepared even when a later tree lacks the clade");
        assertEquals(Integer.valueOf(1), Integer.valueOf(view.preparedAnnotationAnchorCountForTest()),
                "missing clade trees should keep null anchors without failing rendering");
    }

    private static void resolvesNamedInternalNodeConsistencyAnnotation() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(Arrays.asList(
                new ImportedTreeSpec(Path.of("tree-a.nwk"), "Tree A", decodeTree("((Dog:1,Cow:1)Mammal:1,Frog:1);")),
                new ImportedTreeSpec(Path.of("tree-b.nwk"), "Tree B", decodeTree("((Cow:1,Dog:1):1,Frog:1);"))));
        view.setSize(1100, 780);
        view.doLayout();
        view.applyConsistencyAnnotationsForTest(List.of(
                new ConsistencyAnnotation(List.of("Mammal"), new Color(255, 162, 52, 162), 5.0d)));
        view.renderForCurrentSizeForTest();
        waitUntil(view::canExport, 3000, "3D alignment view did not finish rendering with named internal-node annotation");

        List<ConsistencyAnnotation> annotations = view.consistencyAnnotationsForTest();
        assertEquals(Arrays.asList("Cow", "Dog"), annotations.get(0).leafNames(),
                "named internal nodes should resolve to their descendant leaf names");
        assertEquals(Integer.valueOf(2), Integer.valueOf(view.preparedAnnotationAnchorCountForTest()),
                "resolved internal-node annotation should connect the same clade across both trees");
    }

    private static void quickLabelsAndCleansThreeDConsistencyAnnotations() throws Exception {
        ThreeDTreeAlignmentView view = new ThreeDTreeAlignmentView(sampleImportedTrees());

        view.quickLabelConsistencyForTest();
        List<ConsistencyAnnotation> quickLabels = view.consistencyAnnotationsForTest();

        assertEquals(Integer.valueOf(1), Integer.valueOf(quickLabels.size()),
                "sample tree should expose one non-root internal clade for quick labels");
        assertEquals(Arrays.asList("Cow", "Dog"), quickLabels.get(0).leafNames(),
                "quick labels should exclude root and use sorted internal clade leaf names");
        assertEquals(Double.valueOf(5.0d), Double.valueOf(quickLabels.get(0).ribbonWidth()),
                "quick labels should use default ribbon width");

        view.cleanAllLabelsForTest();
        assertEquals(Integer.valueOf(0), Integer.valueOf(view.consistencyAnnotationsForTest().size()),
                "clean all labels should remove every consistency annotation");
    }

    private static void calculatesZeroTreeDifferenceForIdenticalTrees() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = ThreeDTreeAlignmentView.calculateTreeDifferenceMetricsForTest(
                Arrays.asList(
                        new ImportedTreeSpec(Path.of("tree-a.nwk"), "Tree A", decodeTree("((Dog:1,Cow:1):2,Frog:1);")),
                        new ImportedTreeSpec(Path.of("tree-b.nwk"), "Tree B", decodeTree("((Dog:1,Cow:1):2,Frog:1);"))));

        assertNear(0.0d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "identical trees should have zero topology difference");
        assertNear(0.0d, metrics.branchLengthDifferenceIndex(), 1.0e-9d,
                "identical trees should have zero branch-length difference");
        assertEquals(Integer.valueOf(1), Integer.valueOf(metrics.referenceCladeCount()),
                "expected one non-root reference clade");
        assertEquals(Integer.valueOf(1), Integer.valueOf(metrics.recoveredReferenceCladeCount()),
                "expected reference clade to be recovered");
    }

    private static void calculatesZeroTopologyDifferenceForSameCladesWithDifferentChildOrder() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "((Dog:1,Cow:1):2,Frog:1);",
                "(Frog:1,(Cow:1,Dog:1):4);"
        );

        assertNear(0.0d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "child order should not affect clade-signature topology difference");
        assertTrue(metrics.branchLengthDifferenceIndex() > 0.0d,
                "same clade with different branch lengths should still report branch-length difference");
    }

    private static void calculatesTopologyDifferenceForMissingReferenceClades() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = ThreeDTreeAlignmentView.calculateTreeDifferenceMetricsForTest(
                Arrays.asList(
                        new ImportedTreeSpec(Path.of("tree-a.nwk"), "Tree A", decodeTree("((Dog:1,Cow:1):2,Frog:1);")),
                        new ImportedTreeSpec(Path.of("tree-b.nwk"), "Tree B", decodeTree("((Dog:1,Frog:1):2,Cow:1);"))));

        assertNear(1.0d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "missing reference clade should maximize topology difference for that clade");
        assertTrue(Double.isNaN(metrics.branchLengthDifferenceIndex()),
                "branch-length difference should be unavailable when no reference clades match");
        assertEquals(Integer.valueOf(0), Integer.valueOf(metrics.recoveredReferenceCladeCount()),
                "expected no recovered reference clades");
    }

    private static void calculatesMaximumTopologyDifferenceWhenReferenceCladesAreAbsent() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "(((A:1,B:1):1,C:1):1,D:1);",
                "((A:1,C:1):1,(B:1,D:1):1);"
        );

        assertNear(1.0d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "all absent reference clades should produce maximum topology difference");
        assertEquals(Integer.valueOf(2), Integer.valueOf(metrics.referenceCladeCount()),
                "expected two non-root internal reference clades");
        assertEquals(Integer.valueOf(0), Integer.valueOf(metrics.recoveredReferenceCladeCount()),
                "expected no recovered reference clades");
    }

    private static void averagesTopologyDifferenceAcrossMultipleOtherTrees() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "((Dog:1,Cow:1):2,Frog:1);",
                "((Dog:1,Cow:1):2,Frog:1);",
                "((Dog:1,Frog:1):2,Cow:1);"
        );

        assertNear(0.5d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "one recovered and one missing other tree should produce TDI 0.5 for one reference clade");
        assertEquals(Integer.valueOf(1), Integer.valueOf(metrics.recoveredReferenceCladeCount()),
                "reference clade should count as recovered if at least one other tree has it");
    }

    private static void averagesTopologyDifferenceAcrossMultipleReferenceClades() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "(((A:1,B:1):1,C:1):1,D:1);",
                "(((A:1,B:1):1,D:1):1,C:1);"
        );

        assertNear(0.5d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "one recovered and one missing reference clade should average to TDI 0.5");
        assertEquals(Integer.valueOf(2), Integer.valueOf(metrics.referenceCladeCount()),
                "expected two non-root internal reference clades");
        assertEquals(Integer.valueOf(1), Integer.valueOf(metrics.recoveredReferenceCladeCount()),
                "expected one recovered reference clade");
    }

    private static void keepsTopologyDifferenceReferenceBased() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics firstAsReference = treeDifferenceMetrics(
                "(((A:1,B:1):1,C:1):1,D:1);",
                "((A:1,B:1):1,C:1,D:1);"
        );
        ThreeDTreeAlignmentView.TreeDifferenceMetrics secondAsReference = treeDifferenceMetrics(
                "((A:1,B:1):1,C:1,D:1);",
                "(((A:1,B:1):1,C:1):1,D:1);"
        );

        assertNear(0.5d, firstAsReference.topologyDifferenceIndex(), 1.0e-9d,
                "reference tree with two clades should average one recovered and one missing clade");
        assertNear(0.0d, secondAsReference.topologyDifferenceIndex(), 1.0e-9d,
                "less resolved reference tree with only shared non-root clades should have zero topology difference");
    }

    private static void returnsUnavailableMetricsForSingleTree() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics("((A:1,B:1):1,C:1);");

        assertTrue(Double.isNaN(metrics.topologyDifferenceIndex()),
                "single tree should not produce a topology difference index");
        assertTrue(Double.isNaN(metrics.branchLengthDifferenceIndex()),
                "single tree should not produce a branch-length difference index");
        assertEquals(Integer.valueOf(0), Integer.valueOf(metrics.referenceCladeCount()),
                "single tree should have no comparable reference clades");
    }

    private static void returnsUnavailableMetricsForReferenceTreeWithoutInternalClades() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "(A:1,B:1);",
                "(A:1,B:2);"
        );

        assertTrue(Double.isNaN(metrics.topologyDifferenceIndex()),
                "tree with no non-root internal reference clades should not produce TDI");
        assertTrue(Double.isNaN(metrics.branchLengthDifferenceIndex()),
                "tree with no non-root internal reference clades should not produce BDI");
    }

    private static void calculatesBranchLengthDifferenceForMatchedClades() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = ThreeDTreeAlignmentView.calculateTreeDifferenceMetricsForTest(
                Arrays.asList(
                        new ImportedTreeSpec(Path.of("tree-a.nwk"), "Tree A", decodeTree("((Dog:1,Cow:1):2,Frog:1);")),
                        new ImportedTreeSpec(Path.of("tree-b.nwk"), "Tree B", decodeTree("((Dog:1,Cow:1):6,Frog:1);"))));

        assertNear(0.0d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "same topology should have zero topology difference");
        assertTrue(metrics.branchLengthDifferenceIndex() > 0.0d && metrics.branchLengthDifferenceIndex() < 1.0d,
                "branch-length difference should be bounded between zero and one for different matched lengths");
    }

    private static void calculatesExpectedBranchLengthDifferenceForKnownLengths() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "((Dog:1,Cow:1):2,Frog:1);",
                "((Dog:1,Cow:1):6,Frog:1);"
        );

        assertNear(1.0d / 3.0d, metrics.branchLengthDifferenceIndex(), 1.0e-9d,
                "lengths 2 and 6 should produce CV 0.5 and BDI one third");
    }

    private static void averagesBranchLengthDifferenceAcrossMatchedClades() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "(((A:1,B:1):2,C:1):4,D:1);",
                "(((A:1,B:1):6,C:1):4,D:1);"
        );

        assertNear(1.0d / 6.0d, metrics.branchLengthDifferenceIndex(), 1.0e-9d,
                "one clade with BDI one third and one clade with BDI zero should average to one sixth");
    }

    private static void returnsUnavailableBranchLengthDifferenceWithoutMatchedClades() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "((Dog:1,Cow:1):2,Frog:1);",
                "((Dog:1,Frog:1):2,Cow:1);"
        );

        assertNear(1.0d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "missing reference clade should maximize topology difference");
        assertTrue(Double.isNaN(metrics.branchLengthDifferenceIndex()),
                "BDI should be unavailable without matched reference clades");
    }

    private static void boundsBranchLengthDifferenceForExtremeLengths() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "((Dog:1,Cow:1):1,Frog:1);",
                "((Dog:1,Cow:1):1000000,Frog:1);"
        );

        assertTrue(metrics.branchLengthDifferenceIndex() > 0.49d && metrics.branchLengthDifferenceIndex() < 1.0d,
                "finite extreme branch-length differences should stay bounded below one");
    }

    private static void ignoresMissingCladesForBranchLengthDifference() throws Exception {
        ThreeDTreeAlignmentView.TreeDifferenceMetrics metrics = treeDifferenceMetrics(
                "(((A:1,B:1):2,C:1):4,D:1);",
                "(((A:1,B:1):6,D:1):10,C:1);"
        );

        assertNear(0.5d, metrics.topologyDifferenceIndex(), 1.0e-9d,
                "one of two reference clades is missing, so TDI should be 0.5");
        assertNear(1.0d / 3.0d, metrics.branchLengthDifferenceIndex(), 1.0e-9d,
                "BDI should only use matched reference clade lengths");
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

    private static ResizableTanglegramView renderedTanglegramView() throws Exception {
        Path movedOutput = copySampleOutput();
        TreeSummaryLoadResult result = TreeSummaryLoader.load(movedOutput.resolve("tree_summary"));
        TreePairSpec firstPair = result.availablePairs().get(0);
        ResizableTanglegramView view = new ResizableTanglegramView(firstPair, new TanglegramPanelFactory());
        view.setSize(900, 700);
        view.doLayout();
        view.renderNowForTest(new Dimension(900, 700));
        view.doLayout();
        return view;
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

    private static JScrollPane findRequiredScrollPane(Component component) {
        JScrollPane scrollPane = findScrollPane(component);
        if (scrollPane == null) {
            throw new AssertionError("Missing JScrollPane in " + component.getClass().getName());
        }
        return scrollPane;
    }

    private static JScrollPane findScrollPane(Component component) {
        if (component instanceof JScrollPane scrollPane) {
            return scrollPane;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JScrollPane found = findScrollPane(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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

    private static List<String> menuItemLabels(JPopupMenu popupMenu) {
        List<String> labels = new ArrayList<>();
        for (Component component : popupMenu.getComponents()) {
            if (component instanceof JMenuItem menuItem) {
                labels.add(menuItem.getText());
            }
        }
        return labels;
    }

    private static void dispatchWheel(Component component, int x, int y, int wheelRotation) {
        MouseWheelEvent event = new MouseWheelEvent(
                component,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                0,
                Math.max(1, x),
                Math.max(1, y),
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                wheelRotation);
        component.dispatchEvent(event);
    }

    private static void dispatchLeftDrag(Component component, Point start, Point end) {
        long now = System.currentTimeMillis();
        dispatchMouse(component, MouseEvent.MOUSE_PRESSED, now, start.x, start.y, start.x, start.y, MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        dispatchMouse(component, MouseEvent.MOUSE_DRAGGED, now + 1L, end.x, end.y, end.x, end.y, MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON);
        dispatchMouse(component, MouseEvent.MOUSE_RELEASED, now + 2L, end.x, end.y, end.x, end.y, 0, MouseEvent.BUTTON1);
    }

    private static void dispatchMouse(
            Component component,
            int eventId,
            long when,
            int x,
            int y,
            int absoluteX,
            int absoluteY,
            int modifiers,
            int button) {
        component.dispatchEvent(new MouseEvent(
                component,
                eventId,
                when,
                modifiers,
                x,
                y,
                absoluteX,
                absoluteY,
                1,
                false,
                button));
    }

    private static <T> T invokeNoArg(Object target, String methodName, Class<T> returnType) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        Object value = method.invoke(target);
        if (returnType == Object.class) {
            return null;
        }
        return returnType.cast(value);
    }

    private static <T> T invokeWithPoint(Object target, String methodName, Point point, Class<T> returnType) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, Point.class);
        method.setAccessible(true);
        Object value = method.invoke(target, point);
        return returnType.cast(value);
    }

    private static void setPrivateBoolean(Object target, String fieldName, boolean value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(target, value);
    }

    private static <T> T getPrivateField(Object target, String fieldName, Class<T> returnType) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return returnType.cast(field.get(target));
    }

    private static void setPrivateStaticField(Class<?> targetClass, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = targetClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static int countNearBlackPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                int red = (argb >>> 16) & 0xff;
                int green = (argb >>> 8) & 0xff;
                int blue = argb & 0xff;
                if (alpha > 0 && red < 50 && green < 50 && blue < 50) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countNonWhitePixels(BufferedImage image, java.awt.Rectangle area) {
        int count = 0;
        int maxY = Math.min(image.getHeight(), area.y + area.height);
        int maxX = Math.min(image.getWidth(), area.x + area.width);
        for (int y = Math.max(0, area.y); y < maxY; y++) {
            for (int x = Math.max(0, area.x); x < maxX; x++) {
                Color color = new Color(image.getRGB(x, y), true);
                if (color.getAlpha() > 0
                        && (color.getRed() < 245 || color.getGreen() < 245 || color.getBlue() < 245)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void waitUntil(BooleanSupplier condition, long timeoutMillis, String message) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20L);
        }
        throw new AssertionError(message);
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

    private static void assertNear(double expected, double actual, double tolerance, String message) {
        if (Double.isNaN(actual) || Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + " expected=[" + expected + "] actual=[" + actual + "]");
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

    private static EvolNode firstLeaf(EvolNode node) {
        if (node.getChildCount() == 0) {
            return node;
        }
        return firstLeaf(evoltree.struct.util.EvolNodeUtil.getChildrenAt(node, 0));
    }

    private static ThreeDTreeAlignmentView.TreeDifferenceMetrics treeDifferenceMetrics(String... newickTrees) throws Exception {
        List<ImportedTreeSpec> trees = new ArrayList<>();
        for (int index = 0; index < newickTrees.length; index++) {
            trees.add(new ImportedTreeSpec(
                    Path.of("tree-" + index + ".nwk"),
                    "Tree " + index,
                    decodeTree(newickTrees[index])));
        }
        return ThreeDTreeAlignmentView.calculateTreeDifferenceMetricsForTest(trees);
    }

    private static List<ImportedTreeSpec> sampleImportedTrees() throws Exception {
        return Arrays.asList(
                new ImportedTreeSpec(Path.of("tree-a.nwk"), "Tree A", decodeTree("((Dog:1,Cow:1):1,Frog:1);")),
                new ImportedTreeSpec(Path.of("tree-b.nwk"), "Tree B", decodeTree("((Dog:1,Cow:1):1,Frog:1);")));
    }

    private static List<ImportedTreeSpec> sampleImportedTreesWithMissingQuickClade() throws Exception {
        return Arrays.asList(
                new ImportedTreeSpec(Path.of("tree-a.nwk"), "Tree A", decodeTree("((Dog:1,Cow:1):1,Frog:1);")),
                new ImportedTreeSpec(Path.of("tree-b.nwk"), "Tree B", decodeTree("((Dog:1,Frog:1):1,Cow:1);")));
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
