package onebuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

public final class OneBuilderStandaloneTest {
    private OneBuilderStandaloneTest() {
    }

    public static void main(String[] args) throws Exception {
        run("buildsProteinExecutionPlanWithoutAlignment", OneBuilderStandaloneTest::buildsProteinExecutionPlanWithoutAlignment);
        run("buildsDnaExecutionPlanWithAlignment", OneBuilderStandaloneTest::buildsDnaExecutionPlanWithAlignment);
        run("serializesPipelineRuntimeConfigAsJson", OneBuilderStandaloneTest::serializesPipelineRuntimeConfigAsJson);
        run("detectsPlatformSupport", OneBuilderStandaloneTest::detectsPlatformSupport);
        run("tracksWorkflowTabUnlocks", OneBuilderStandaloneTest::tracksWorkflowTabUnlocks);
        run("buildsLinuxWorkflowTabs", OneBuilderStandaloneTest::buildsLinuxWorkflowTabs);
        run("buildsWindowsWorkflowTabs", OneBuilderStandaloneTest::buildsWindowsWorkflowTabs);
        run("loadsCurrentRunTanglegramTabs", OneBuilderStandaloneTest::loadsCurrentRunTanglegramTabs);
        run("detectsCurrentRunTanglegramArtifacts", OneBuilderStandaloneTest::detectsCurrentRunTanglegramArtifacts);
        run("interpretsMethodProgressFromLogs", OneBuilderStandaloneTest::interpretsMethodProgressFromLogs);
    }

    private static void buildsProteinExecutionPlanWithoutAlignment() {
        Path scriptDir = Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1");
        Path inputFile = Paths.get("/data/input/aligned.fasta");
        Path outputDir = Paths.get("/data/output");
        Path configPath = Paths.get("/tmp/run-config.json");

        RunRequest request = RunRequest.builder()
                .inputType(InputType.PROTEIN)
                .inputFile(inputFile)
                .outputDirectory(outputDir)
                .outputPrefix("protein_demo")
                .exportConfigFile(true)
                .runAlignmentFirst(false)
                .alignOptions(AlignmentOptions.defaults())
                .runtimeConfig(PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN))
                .build();

        ExecutionPlan executionPlan = new ExecutionPlanBuilder(scriptDir).build(request, configPath);

        assertTrue(executionPlan.alignCommand().isEmpty(), "alignment command should be absent");
        assertEquals(
                Arrays.asList(
                        "/bin/zsh",
                        scriptDir.resolve("s2_phylo_4prot.zsh").toString(),
                        "--config",
                        configPath.toString(),
                        inputFile.toString(),
                        outputDir.resolve("protein_demo").toString()),
                executionPlan.buildCommand(),
                "unexpected protein build command");
        assertEquals(
                outputDir.resolve("protein_demo"),
                executionPlan.pipelineOutputDir(),
                "unexpected protein output directory");
    }

    private static void buildsDnaExecutionPlanWithAlignment() {
        Path scriptDir = Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1");
        Path inputFile = Paths.get("/data/input/raw_sequences.fa");
        Path outputDir = Paths.get("/data/output");
        Path configPath = Paths.get("/tmp/run-config.json");

        RunRequest request = RunRequest.builder()
                .inputType(InputType.DNA_CDS)
                .inputFile(inputFile)
                .outputDirectory(outputDir)
                .outputPrefix("dna_demo")
                .exportConfigFile(true)
                .runAlignmentFirst(true)
                .alignOptions(new AlignmentOptions("localpair", 2000, false))
                .runtimeConfig(PipelineRuntimeConfig.defaultsFor(InputType.DNA_CDS))
                .build();

        ExecutionPlan executionPlan = new ExecutionPlanBuilder(scriptDir).build(request, configPath);

        assertEquals(
                Arrays.asList(
                        "/bin/zsh",
                        scriptDir.resolve("s1_quick_align.zsh").toString(),
                        "--strategy",
                        "localpair",
                        "--maxiterate",
                        "2000",
                        "--no-reorder",
                        inputFile.toString()),
                executionPlan.alignCommand().orElseThrow(),
                "unexpected alignment command");
        assertEquals(
                inputFile.resolveSibling("raw_sequences.aligned.fa"),
                executionPlan.effectiveInputFile(),
                "unexpected aligned output path");
        assertEquals(
                Arrays.asList(
                        "/bin/zsh",
                        scriptDir.resolve("s2_phylo_4dna.zsh").toString(),
                        "--config",
                        configPath.toString(),
                        inputFile.resolveSibling("raw_sequences.aligned.fa").toString(),
                        outputDir.resolve("dna_demo").toString()),
                executionPlan.buildCommand(),
                "unexpected dna build command");
    }

    private static void serializesPipelineRuntimeConfigAsJson() throws Exception {
        Path tempFile = Files.createTempFile("onebuilder-config-", ".json");
        PipelineRuntimeConfig config = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN)
                .withMaximumLikelihood(new MaximumLikelihoodConfig(true, 2000, "MFP", "LG,WAG,JTT"))
                .withBayesian(new BayesianConfig(true, "mixed", "gamma", 75000, 250, 1000, 3000))
                .withDistance(new SimpleMethodConfig(false))
                .withParsimony(new SimpleMethodConfig(true));

        RunRequest request = RunRequest.builder()
                .inputType(InputType.PROTEIN)
                .inputFile(Paths.get("/data/input/aligned.fasta"))
                .outputDirectory(Paths.get("/data/output"))
                .outputPrefix("protein_demo")
                .exportConfigFile(true)
                .runAlignmentFirst(false)
                .alignOptions(AlignmentOptions.defaults())
                .runtimeConfig(config)
                .build();

        new PipelineConfigWriter().write(tempFile, request);
        String json = Files.readString(tempFile, StandardCharsets.UTF_8);

        assertTrue(json.contains("\"run\""), "expected run section");
        assertTrue(json.contains("\"output_prefix\": \"protein_demo\""), "expected output prefix");
        assertTrue(json.contains("\"alignment\""), "expected alignment section");
        assertTrue(json.contains("\"run_alignment_first\": false"), "expected alignment flag");
        assertTrue(json.contains("\"maximum_likelihood\""), "expected ML section");
        assertTrue(json.contains("\"bootstrap_replicates\": 2000"), "expected bootstrap count");
        assertTrue(json.contains("\"model_set\": \"LG,WAG,JTT\""), "expected ML model set");
        assertTrue(json.contains("\"bayesian\""), "expected bayesian section");
        assertTrue(json.contains("\"ngen\": 75000"), "expected bayesian ngen");
        assertTrue(json.contains("\"distance\""), "expected distance section");
        assertTrue(json.contains("\"enabled\": false"), "expected distance enabled flag");
    }

    private static void detectsPlatformSupport() {
        assertEquals(PlatformSupport.LINUX, PlatformSupport.detect("Linux"), "unexpected linux detection");
        assertEquals(PlatformSupport.WINDOWS, PlatformSupport.detect("Windows 11"), "unexpected windows detection");
        assertEquals(PlatformSupport.OTHER, PlatformSupport.detect("Mac OS X"), "unexpected fallback detection");
        assertTrue(PlatformSupport.LINUX.supportsPipelineExecution(), "linux should support pipeline execution");
        assertTrue(!PlatformSupport.WINDOWS.supportsPipelineExecution(), "windows should not support pipeline execution");
    }

    private static void tracksWorkflowTabUnlocks() {
        WorkflowTabsState state = WorkflowTabsState.initial();
        assertTrue(state.inputEnabled(), "input tab should be enabled initially");
        assertTrue(!state.treeBuildEnabled(), "tree build tab should start disabled");
        assertTrue(!state.tanglegramEnabled(), "tanglegram tab should start disabled");

        state = state.markInputConfigured();
        assertTrue(state.treeBuildEnabled(), "tree build tab should unlock when input is configured");
        assertTrue(!state.tanglegramEnabled(), "tanglegram tab should still be locked");

        state = state.markRunStarted();
        state = state.markTanglegramReady();
        assertTrue(state.tanglegramEnabled(), "tanglegram tab should unlock when results are ready");
    }

    private static void buildsLinuxWorkflowTabs() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            OneBuilderWorkspacePanel workspacePanel = new OneBuilderWorkspacePanel(
                    Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1"),
                    PlatformSupport.LINUX);
            assertEquals(3, workspacePanel.workflowTabs().getTabCount(), "unexpected top-level tab count");
            assertEquals("Input / Align", workspacePanel.workflowTabs().getTitleAt(0), "unexpected first tab");
            assertEquals("Tree Build", workspacePanel.workflowTabs().getTitleAt(1), "unexpected second tab");
            assertEquals("Tanglegram", workspacePanel.workflowTabs().getTitleAt(2), "unexpected third tab");
            assertTrue(!workspacePanel.workflowTabs().isEnabledAt(1), "tree build tab should start disabled");
            assertTrue(!workspacePanel.workflowTabs().isEnabledAt(2), "tanglegram tab should start disabled");
            assertTrue(workspacePanel.inputAlignPanel().isRunSupported(), "linux should support run");
            assertTrue(workspacePanel.inputAlignPanel().isExportSelected(), "export should default to selected");
            assertEquals(4, workspacePanel.treeBuildPanel().methodTabs().getTabCount(), "unexpected method tab count");
            assertEquals("Distance", workspacePanel.treeBuildPanel().methodTabs().getTitleAt(0), "unexpected distance tab");
            assertEquals("ML", workspacePanel.treeBuildPanel().methodTabs().getTitleAt(1), "unexpected ml tab");
            assertEquals("Bayesian", workspacePanel.treeBuildPanel().methodTabs().getTitleAt(2), "unexpected bayesian tab");
            assertEquals("Parsimony", workspacePanel.treeBuildPanel().methodTabs().getTitleAt(3), "unexpected parsimony tab");
        });
    }

    private static void buildsWindowsWorkflowTabs() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            OneBuilderWorkspacePanel workspacePanel = new OneBuilderWorkspacePanel(
                    Paths.get("C:/onebuilder/phylotree_builder_v0.0.1"),
                    PlatformSupport.WINDOWS);
            assertTrue(!workspacePanel.inputAlignPanel().isRunSupported(), "windows should disable run");
            assertTrue(workspacePanel.inputAlignPanel().isExportButtonEnabled(), "windows should allow export");
            assertTrue(!workspacePanel.workflowTabs().isEnabledAt(2), "windows should keep tanglegram tab disabled");
        });
    }

    private static void loadsCurrentRunTanglegramTabs() throws Exception {
        Path repoRoot = findRepoRoot();
        Path sampleOutput = repoRoot.resolve("test1");

        SwingUtilities.invokeAndWait(() -> {
            CurrentRunTanglegramPanel panel = new CurrentRunTanglegramPanel();
            panel.loadRunResults(sampleOutput);
            assertEquals(6, panel.comparisonTabs().getTabCount(), "unexpected tanglegram pair tab count");
            assertEquals("NJ-ML", panel.comparisonTabs().getTitleAt(0), "unexpected first pair tab");
            assertEquals("NJ-BI", panel.comparisonTabs().getTitleAt(1), "unexpected second pair tab");
            assertEquals("NJ-MP", panel.comparisonTabs().getTitleAt(2), "unexpected third pair tab");
            assertEquals("ML-BI", panel.comparisonTabs().getTitleAt(3), "unexpected fourth pair tab");
            assertEquals("ML-MP", panel.comparisonTabs().getTitleAt(4), "unexpected fifth pair tab");
            assertEquals("BI-MP", panel.comparisonTabs().getTitleAt(5), "unexpected sixth pair tab");
        });
    }

    private static void detectsCurrentRunTanglegramArtifacts() {
        Path repoRoot = findRepoRoot();
        Path sampleOutput = repoRoot.resolve("test1");

        assertTrue(CurrentRunArtifacts.hasRenderableTanglegram(sampleOutput), "expected sample output to be renderable");
        assertEquals(sampleOutput.resolve("tree_summary"), CurrentRunArtifacts.resolveTreeSummaryDir(sampleOutput),
                "unexpected tree summary directory");
    }

    private static void interpretsMethodProgressFromLogs() {
        PipelineProgressInterpreter interpreter = new PipelineProgressInterpreter();

        assertEquals(
                MethodProgressEvent.running(TreeMethodKey.MAXIMUM_LIKELIHOOD),
                interpreter.interpret(InputType.PROTEIN, "2026-04-12 INFO Starting maximum likelihood inference (IQ-TREE)..."),
                "unexpected running event");
        assertEquals(
                MethodProgressEvent.completed(TreeMethodKey.BAYESIAN),
                interpreter.interpret(InputType.PROTEIN, "====Bayesian method complete================"),
                "unexpected completed event");
        assertEquals(
                MethodProgressEvent.running(TreeMethodKey.BAYESIAN),
                interpreter.interpret(InputType.DNA_CDS, "2026-04-12 - INFO - 开始贝叶斯法建树..."),
                "unexpected dna running event");
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
