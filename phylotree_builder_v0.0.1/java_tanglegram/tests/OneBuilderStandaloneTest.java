package onebuilder;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;
import org.json.JSONObject;
import tanglegram.UiLanguage;
import tanglegram.UiPreferenceStore;
import tanglegram.UiPreferences;

public final class OneBuilderStandaloneTest {
    private static final String SUITE_PREFERENCE_NODE = "/egps-onebuilder/tests/onebuilder/suite";

    private OneBuilderStandaloneTest() {
    }

    public static void main(String[] args) throws Exception {
        UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
        UiPreferenceStore.clearNodeForTests();
        try {
            run("buildsProteinExecutionPlanWithoutAlignment", OneBuilderStandaloneTest::buildsProteinExecutionPlanWithoutAlignment);
            run("buildsExecutionPlanWithOverwriteFlag", OneBuilderStandaloneTest::buildsExecutionPlanWithOverwriteFlag);
            run("buildsDnaExecutionPlanWithAlignment", OneBuilderStandaloneTest::buildsDnaExecutionPlanWithAlignment);
            run("serializesPipelineRuntimeConfigAsJson", OneBuilderStandaloneTest::serializesPipelineRuntimeConfigAsJson);
            run("roundTripsProteinStructureTreeBuilderMethod", OneBuilderStandaloneTest::roundTripsProteinStructureTreeBuilderMethod);
            run("proteinStructureTreeCommandBuildsNewick", OneBuilderStandaloneTest::proteinStructureTreeCommandBuildsNewick);
            run("serializesRerootRuntimeConfigAsJson", OneBuilderStandaloneTest::serializesRerootRuntimeConfigAsJson);
            run("defaultsRerootPanelToVisibleLadderizationRules", OneBuilderStandaloneTest::defaultsRerootPanelToVisibleLadderizationRules);
            run("rerootTreeCommandUsesEgpsMidpointRooting", OneBuilderStandaloneTest::rerootTreeCommandUsesEgpsMidpointRooting);
            run("treePostprocessCommandUsesEgpsTreeUtilities", OneBuilderStandaloneTest::treePostprocessCommandUsesEgpsTreeUtilities);
            run("detectsPlatformSupport", OneBuilderStandaloneTest::detectsPlatformSupport);
            run("tracksWorkflowTabUnlocks", OneBuilderStandaloneTest::tracksWorkflowTabUnlocks);
            run("relocksTanglegramWhenNewRunStarts", OneBuilderStandaloneTest::relocksTanglegramWhenNewRunStarts);
            run("buildsStandaloneTanglegramLaunchCommand", OneBuilderStandaloneTest::buildsStandaloneTanglegramLaunchCommand);
            run("buildsLinuxWorkbenchShell", OneBuilderStandaloneTest::buildsLinuxWorkbenchShell);
            run("buildsWindowsWorkbenchShell", OneBuilderStandaloneTest::buildsWindowsWorkbenchShell);
            run("blocksNavigationUntilRequiredInputIsReady", OneBuilderStandaloneTest::blocksNavigationUntilRequiredInputIsReady);
            run("prefersInputParentDirectoryWhenInputPathIsAFile", OneBuilderStandaloneTest::prefersInputParentDirectoryWhenInputPathIsAFile);
            run("rejectsInvalidOutputDirectoryPath", OneBuilderStandaloneTest::rejectsInvalidOutputDirectoryPath);
            run("rejectsRunWhenAllTreeMethodsAreDisabled", OneBuilderStandaloneTest::rejectsRunWhenAllTreeMethodsAreDisabled);
            run("countsEnabledTreeMethodsForSingleMethodWarning", OneBuilderStandaloneTest::countsEnabledTreeMethodsForSingleMethodWarning);
            run("requiresProstt5ModelPathForFastaOnlyProteinStructure", OneBuilderStandaloneTest::requiresProstt5ModelPathForFastaOnlyProteinStructure);
            run("autoDetectsDnaInputTypeForExportedConfig", OneBuilderStandaloneTest::autoDetectsDnaInputTypeForExportedConfig);
            run("autoEnablesAlignmentForUnequalSequenceLengths", OneBuilderStandaloneTest::autoEnablesAlignmentForUnequalSequenceLengths);
            run("adaptsMaximumLikelihoodStrategiesByInputType", OneBuilderStandaloneTest::adaptsMaximumLikelihoodStrategiesByInputType);
            run("showsBootstrapGuidanceForTypicalAndExtremeValues", OneBuilderStandaloneTest::showsBootstrapGuidanceForTypicalAndExtremeValues);
            run("preservesAlrtZeroInSerializedConfig", OneBuilderStandaloneTest::preservesAlrtZeroInSerializedConfig);
            run("omitsStopvalWhenStopruleIsDisabled", OneBuilderStandaloneTest::omitsStopvalWhenStopruleIsDisabled);
            run("roundTripsAdvancedRuntimeConfigThroughPanels", OneBuilderStandaloneTest::roundTripsAdvancedRuntimeConfigThroughPanels);
            run("usesPreferenceDefaultForEmbeddedTanglegramLabelSize", OneBuilderStandaloneTest::usesPreferenceDefaultForEmbeddedTanglegramLabelSize);
            run("loadsCurrentRunTanglegramTabs", OneBuilderStandaloneTest::loadsCurrentRunTanglegramTabs);
            run("detectsCurrentRunTanglegramArtifacts", OneBuilderStandaloneTest::detectsCurrentRunTanglegramArtifacts);
            run("interpretsMethodProgressFromLogs", OneBuilderStandaloneTest::interpretsMethodProgressFromLogs);
            run("tracksTreeBuildOverallProgressFromEnabledMethodsAndLogs", OneBuilderStandaloneTest::tracksTreeBuildOverallProgressFromEnabledMethodsAndLogs);
            run("keepsTreeBuildCaretAtEndAfterAppendingLogs", OneBuilderStandaloneTest::keepsTreeBuildCaretAtEndAfterAppendingLogs);
            run("storesLanguageInExportedConfig", OneBuilderStandaloneTest::storesLanguageInExportedConfig);
            run("remembersInputAlignBrowseDirectories", OneBuilderStandaloneTest::remembersInputAlignBrowseDirectories);
            run("usesGlobalFontForCitationText", OneBuilderStandaloneTest::usesGlobalFontForCitationText);
            run("showsTanglegramSnapshotChipBeforeStatus", OneBuilderStandaloneTest::showsTanglegramSnapshotChipBeforeStatus);
        } finally {
            UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
            UiPreferenceStore.clearNodeForTests();
            UiPreferenceStore.resetNodeForTests();
        }
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
                        executionPlan.pipelineOutputDir().toString()),
                executionPlan.buildCommand(),
                "unexpected protein build command");
        assertEquals(
                outputDir.resolve("protein_demo").toAbsolutePath().normalize(),
                executionPlan.pipelineOutputDir(),
                "unexpected protein output directory");
    }

    private static void buildsExecutionPlanWithOverwriteFlag() {
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
                .overwriteExistingOutput(true)
                .runAlignmentFirst(false)
                .alignOptions(AlignmentOptions.defaults())
                .runtimeConfig(PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN))
                .build();

        ExecutionPlan executionPlan = new ExecutionPlanBuilder(scriptDir).build(request, configPath);

        assertEquals(
                Arrays.asList(
                        "/bin/zsh",
                        scriptDir.resolve("s2_phylo_4prot.zsh").toString(),
                        "--force-overwrite",
                        "--config",
                        configPath.toString(),
                        inputFile.toString(),
                        executionPlan.pipelineOutputDir().toString()),
                executionPlan.buildCommand(),
                "unexpected overwrite-enabled build command");
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
                .alignOptions(new AlignmentOptions("localpair", 2000, false, Arrays.asList("--thread", "4")))
                .runtimeConfig(PipelineRuntimeConfig.defaultsFor(InputType.DNA_CDS))
                .build();

        ExecutionPlan executionPlan = new ExecutionPlanBuilder(scriptDir).build(request, configPath);

        assertEquals(
                Arrays.asList(
                        "/bin/zsh",
                        scriptDir.resolve("s1_quick_align.zsh").toString(),
                        "--config",
                        configPath.toString(),
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
                        executionPlan.pipelineOutputDir().toString()),
                executionPlan.buildCommand(),
                "unexpected dna build command");
    }

    private static void serializesPipelineRuntimeConfigAsJson() throws Exception {
        Path tempFile = Files.createTempFile("onebuilder-config-", ".json");
        PipelineRuntimeConfig config = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN)
                .withMaximumLikelihood(new MaximumLikelihoodConfig(
                        true,
                        2000,
                        "MFP",
                        "LG,WAG,JTT",
                        "AUTO",
                        Integer.valueOf(12),
                        Integer.valueOf(42),
                        true,
                        false,
                        true,
                        false,
                        true,
                        "8G",
                        "seq1",
                        "AA",
                        Integer.valueOf(1000),
                        true,
                        Arrays.asList("-bnni", "-wbtl")))
                .withBayesian(new BayesianConfig(
                        true,
                        "mixed",
                        "gamma",
                        75000,
                        250,
                        1000,
                        3000,
                        null,
                        Integer.valueOf(2),
                        Integer.valueOf(4),
                        Double.valueOf(0.2),
                        Boolean.TRUE,
                        Double.valueOf(0.01),
                        Integer.valueOf(500),
                        Double.valueOf(0.25),
                        Boolean.TRUE,
                        Arrays.asList("charset core = 1-300;", "prset statefreqpr=fixed(equal);")))
                .withDistance(new SimpleMethodConfig(
                        false,
                        "F84",
                        Double.valueOf(2.0d),
                        true,
                        "NJ",
                        Integer.valueOf(3),
                        null,
                        null,
                        false,
                        Arrays.asList("P", "P", "G", "0.5", "Y"),
                        java.util.Collections.emptyList(),
                        Arrays.asList("N", "Y"),
                        java.util.Collections.emptyList(),
                        java.util.Collections.emptyList()))
                .withParsimony(new SimpleMethodConfig(
                        true,
                        "F84",
                        Double.valueOf(2.0d),
                        true,
                        "NJ",
                        null,
                        Integer.valueOf(7),
                        null,
                        false,
                        false,
                        true,
                        java.util.Collections.emptyList(),
                        java.util.Collections.emptyList(),
                        java.util.Collections.emptyList(),
                        Arrays.asList("J", "7", "Y"),
                        java.util.Collections.emptyList()))
                .withProteinStructure(new ProteinStructureConfig(
                        true,
                        true,
                        "/data/structures.tsv",
                        "/models/prostt5",
                        "SwiftNJ",
                        6,
                        7.5d,
                        1.0d,
                        500,
                        0.4d,
                        2,
                        1,
                        0.6d,
                        true,
                        true,
                        false,
                        2,
                        Arrays.asList("--prefilter-mode", "2")));

        RunRequest request = RunRequest.builder()
                .inputType(InputType.PROTEIN)
                .inputFile(Paths.get("/data/input/aligned.fasta"))
                .outputDirectory(Paths.get("/data/output"))
                .outputPrefix("protein_demo")
                .exportConfigFile(true)
                .runAlignmentFirst(false)
                .alignOptions(new AlignmentOptions("globalpair", 3000, Integer.valueOf(8), false, Arrays.asList("--retree", "2")))
                .runtimeConfig(config)
                .build();

        new PipelineConfigWriter().write(tempFile, request);
        String json = Files.readString(tempFile, StandardCharsets.UTF_8);
        JSONObject root = new JSONObject(json);
        assertTrue(root.has("alignment"), "expected valid JSON root");
        assertTrue(root.getJSONObject("methods").getJSONObject("maximum_likelihood").getJSONObject("iqtree").getJSONArray("extra_args").length() == 2,
                "expected two IQ-TREE extra args");
        assertTrue(root.getJSONObject("methods").getJSONObject("bayesian").getJSONObject("mrbayes").getJSONArray("command_block").length() == 2,
                "expected two MrBayes passthrough commands");
        assertTrue(root.getJSONObject("methods").getJSONObject("protein_structure").getBoolean("enabled"),
                "expected protein structure config to be enabled");
        assertEquals("/data/structures.tsv",
                root.getJSONObject("methods").getJSONObject("protein_structure").getString("structure_manifest_file"),
                "expected protein structure TSV path");
        assertEquals("/models/prostt5",
                root.getJSONObject("methods").getJSONObject("protein_structure").getString("prostt5_model_path"),
                "expected ProstT5 model path");
        assertEquals("mean_qtmscore_ttmscore",
                root.getJSONObject("methods").getJSONObject("protein_structure").getString("similarity_rule"),
                "expected default Foldseek similarity rule");
        assertEquals("1",
                root.getJSONObject("methods").getJSONObject("protein_structure").getString("missing_distance"),
                "expected explicit missing Foldseek pair distance policy");
        assertEquals("SwiftNJ",
                root.getJSONObject("methods").getJSONObject("protein_structure").getString("tree_builder_method"),
                "expected protein structure tree builder method");
        JSONObject foldseek = root.getJSONObject("methods").getJSONObject("protein_structure").getJSONObject("foldseek");
        assertEquals(Integer.valueOf(6), Integer.valueOf(foldseek.getJSONObject("common").getInt("threads")),
                "expected Foldseek thread count");
        assertEquals(Double.valueOf(7.5d), Double.valueOf(foldseek.getJSONObject("common").getDouble("sensitivity")),
                "expected Foldseek sensitivity");
        assertEquals(Integer.valueOf(500), Integer.valueOf(foldseek.getJSONObject("common").getInt("max_seqs")),
                "expected Foldseek max seqs");
        assertTrue(foldseek.getJSONObject("advanced").getBoolean("exhaustive_search"),
                "expected Foldseek exhaustive search flag");
        assertTrue(foldseek.getJSONObject("advanced").getBoolean("exact_tmscore"),
                "expected Foldseek exact TM-score flag");
        assertEquals(Integer.valueOf(2), Integer.valueOf(foldseek.getJSONArray("extra_args").length()),
                "expected Foldseek extra args");

        assertTrue(json.contains("\"run\""), "expected run section");
        assertTrue(json.contains("\"output_prefix\": \"protein_demo\""), "expected output prefix");
        assertTrue(json.contains("\"language\": \"english\""), "expected default UI language in run section");
        assertTrue(json.contains("\"alignment\""), "expected alignment section");
        assertTrue(json.contains("\"run_alignment_first\": false"), "expected alignment flag");
        assertTrue(json.contains("\"reroot\""), "expected reroot section");
        assertTrue(json.contains("\"method\": \"MAD\""), "expected default MAD reroot method");
        assertTrue(json.contains("\"mafft\""), "expected MAFFT section");
        assertTrue(json.contains("\"common\""), "expected common tier");
        assertTrue(json.contains("\"advanced\""), "expected advanced tier");
        assertTrue(json.contains("\"strategy\": \"globalpair\""), "expected alignment strategy");
        assertTrue(json.contains("\"threads\": 8"), "expected MAFFT thread count");
        assertTrue(json.contains("\"extra_args\": ["), "expected extra args array");
        assertTrue(json.contains("\"--retree\""), "expected MAFFT extra arg");
        assertTrue(json.contains("\"maximum_likelihood\""), "expected ML section");
        assertTrue(json.contains("\"iqtree\""), "expected IQ-TREE section");
        assertTrue(json.contains("\"bootstrap_replicates\": 2000"), "expected bootstrap count");
        assertTrue(json.contains("\"model_set\": \"LG,WAG,JTT\""), "expected ML model set");
        assertTrue(json.contains("\"threads\": \"AUTO\""), "expected IQ-TREE threads");
        assertTrue(json.contains("\"threads_max\": 12"), "expected IQ-TREE thread cap");
        assertTrue(json.contains("\"seed\": 42"), "expected IQ-TREE seed");
        assertTrue(json.contains("\"memory_limit\": \"8G\""), "expected IQ-TREE memory limit");
        assertTrue(json.contains("\"outgroup\": \"seq1\""), "expected IQ-TREE outgroup");
        assertTrue(json.contains("\"alrt\": 1000"), "expected IQ-TREE alrt support");
        assertTrue(json.contains("\"-bnni\""), "expected IQ-TREE passthrough arg");
        assertTrue(json.contains("\"bayesian\""), "expected bayesian section");
        assertTrue(json.contains("\"mrbayes\""), "expected MrBayes section");
        assertTrue(json.contains("\"ngen\": 75000"), "expected bayesian ngen");
        assertTrue(json.contains("\"nruns\": 2"), "expected MrBayes nruns");
        assertTrue(json.contains("\"nchains\": 4"), "expected MrBayes nchains");
        assertTrue(json.contains("\"burninfrac\": 0.25"), "expected MrBayes burnin fraction");
        assertTrue(json.contains("\"charset core = 1-300;\""), "expected MrBayes passthrough command");
        assertTrue(json.contains("\"distance\""), "expected distance section");
        assertTrue(json.contains("\"protdist\""), "expected protdist section");
        assertTrue(json.contains("\"neighbor\""), "expected neighbor section");
        assertTrue(json.contains("\"neighbor_method\": \"NJ\""), "expected neighbor method");
        assertTrue(json.contains("\"neighbor_outgroup_index\": 3"), "expected neighbor outgroup");
        assertTrue(json.contains("\"protpars_outgroup_index\": 7"), "expected protpars outgroup");
        assertTrue(json.contains("\"protpars_print_steps\": false"), "expected protpars print-steps flag");
        assertTrue(json.contains("\"protpars_print_sequences\": true"), "expected protpars print-sequences flag");
        assertTrue(json.contains("\"menu_overrides\": ["), "expected PHYLIP passthrough array");
        assertTrue(json.contains("\"P\""), "expected PHYLIP menu override");
        assertTrue(json.contains("\"enabled\": false"), "expected distance enabled flag");
    }

    private static void roundTripsProteinStructureTreeBuilderMethod() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProteinStructurePanel panel = new ProteinStructurePanel(InputType.PROTEIN);

            assertEquals("NJ", panel.toConfig().treeBuilderMethod(), "expected NJ as the default structure tree builder");

            panel.apply(new ProteinStructureConfig(true, false, null, "/models/prostt5", "SwiftNJ"));
            assertEquals("SwiftNJ", panel.toConfig().treeBuilderMethod(), "expected SwiftNJ to round-trip through the panel");
            assertEquals("/models/prostt5", panel.toConfig().prostt5ModelPath(), "expected ProstT5 model path to round-trip through the panel");
        });
    }

    private static void proteinStructureTreeCommandBuildsNewick() throws Exception {
        Path tempDirectory = Files.createTempDirectory("protein-structure-tree-");
        Path matrixFile = tempDirectory.resolve("distance_matrix.tsv");
        Path outputFile = tempDirectory.resolve("structure_tree.nwk");
        Files.writeString(matrixFile,
                "id\tseqA\tseqB\tseqC\n"
                        + "seqA\t0\t0.3\t0.5\n"
                        + "seqB\t0.3\t0\t0.4\n"
                        + "seqC\t0.5\t0.4\t0\n",
                StandardCharsets.UTF_8);

        int exitCode = ProteinStructureTreeCommand.run(new String[] {
                "--method", "SwiftNJ",
                "--input", matrixFile.toString(),
                "--output", outputFile.toString()
        });

        assertEquals(Integer.valueOf(0), Integer.valueOf(exitCode), "expected structure tree command to succeed");
        String newick = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(newick.trim().endsWith(";"), "expected Newick terminator");
        assertTrue(newick.contains("seqA"), "expected seqA leaf");
        assertTrue(newick.contains("seqB"), "expected seqB leaf");
        assertTrue(newick.contains("seqC"), "expected seqC leaf");
    }

    private static void serializesRerootRuntimeConfigAsJson() throws Exception {
        Path tempFile = Files.createTempFile("onebuilder-reroot-config-", ".json");
        PipelineRuntimeConfig config = PipelineRuntimeConfig.defaultsFor(InputType.DNA_CDS)
                .withReroot(new RerootConfig(RerootMethod.ROOT_AT_MIDDLE_POINT, LadderizeDirection.DOWN, true, true));

        RunRequest request = RunRequest.builder()
                .inputType(InputType.DNA_CDS)
                .inputFile(Paths.get("/data/input/aligned.fasta"))
                .outputDirectory(Paths.get("/data/output"))
                .outputPrefix("dna_demo")
                .exportConfigFile(true)
                .runAlignmentFirst(false)
                .alignOptions(AlignmentOptions.defaults())
                .runtimeConfig(config)
                .build();

        new PipelineConfigWriter().write(tempFile, request);
        JSONObject root = new JSONObject(Files.readString(tempFile, StandardCharsets.UTF_8));
        assertEquals(
                "root-at-middle-point",
                root.getJSONObject("reroot").getString("method"),
                "unexpected midpoint reroot JSON value");
        JSONObject ladderization = root.getJSONObject("reroot").getJSONObject("ladderization");
        assertEquals("DOWN", ladderization.getString("direction"), "unexpected ladderization direction");
        assertTrue(ladderization.getBoolean("sort_by_clade_size"), "expected clade-size sorting flag");
        assertTrue(ladderization.getBoolean("sort_by_branch_length"), "expected branch-length sorting flag");
    }

    private static void defaultsRerootPanelToVisibleLadderizationRules() {
        RerootTreePanel panel = new RerootTreePanel();
        RerootConfig config = panel.toConfig();
        assertEquals(RerootMethod.MAD, config.method(), "expected MAD as the default reroot method");
        assertEquals(LadderizeDirection.UP, config.ladderizeDirection(), "expected UP as the default ladderization direction");
        assertTrue(config.sortByCladeSize(), "expected clade-size sorting to stay enabled");
        assertTrue(config.sortByBranchLength(), "expected branch-length sorting to stay enabled");
    }

    private static void rerootTreeCommandUsesEgpsMidpointRooting() throws Exception {
        Path tempDirectory = Files.createTempDirectory("onebuilder-midpoint-");
        Path inputTree = tempDirectory.resolve("input.nwk");
        Path outputTree = tempDirectory.resolve("output.nwk");
        Files.writeString(inputTree, "((A:1,B:1):1,C:4);", StandardCharsets.UTF_8);

        int exitCode = RerootTreeCommand.run(new String[] {
                "--method", "root-at-middle-point",
                "--input", inputTree.toString(),
                "--output", outputTree.toString()
        });

        assertEquals(Integer.valueOf(0), Integer.valueOf(exitCode), "midpoint reroot command should succeed");
        assertTrue(Files.exists(outputTree), "expected midpoint output tree");
        String output = Files.readString(outputTree, StandardCharsets.UTF_8).trim();
        assertTrue(output.endsWith(";"), "expected Newick output");
        assertTrue(output.contains("A") && output.contains("B") && output.contains("C"),
                "expected all leaves to be preserved");
    }

    private static void treePostprocessCommandUsesEgpsTreeUtilities() throws Exception {
        Path tempDirectory = Files.createTempDirectory("onebuilder-tree-postprocess-");
        Path inputTree = tempDirectory.resolve("input.nwk");
        Path outputTree = tempDirectory.resolve("output.nwk");
        Path renameMap = tempDirectory.resolve("rename_map.tsv");
        Files.writeString(inputTree, "((seq1:-2,seq2:3):4,seq3:5);", StandardCharsets.UTF_8);
        Files.writeString(renameMap, "seq1\tHuman\nseq2\tMouse\nseq3\tDog\n", StandardCharsets.UTF_8);

        int exitCode = TreePostprocessCommand.run(new String[] {
                "--input", inputTree.toString(),
                "--output", outputTree.toString(),
                "--rename-map", renameMap.toString(),
                "--clamp-negative-branch-lengths",
                "--set-all-branch-lengths", "1",
                "--ladderize-direction", "UP",
                "--sort-by-clade-size",
                "--sort-by-branch-length"
        });

        assertEquals(Integer.valueOf(0), Integer.valueOf(exitCode), "tree postprocess command should succeed");
        String output = Files.readString(outputTree, StandardCharsets.UTF_8).trim();
        assertTrue(output.endsWith(";"), "expected Newick output");
        assertTrue(output.contains("Human") && output.contains("Mouse") && output.contains("Dog"),
                "expected renamed leaves in output tree");
        assertTrue(!output.contains("seq1") && !output.contains("seq2") && !output.contains("seq3"),
                "expected indexed names to be removed");
        assertTrue(!output.contains(":-"), "expected negative branch lengths to be clamped");
        assertTrue(output.contains(":1"), "expected branch lengths to be rewritten to one");
        assertTrue(!output.contains(":3") && !output.contains(":4") && !output.contains(":5"),
                "expected original branch lengths to be replaced");
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
        assertTrue(!state.treeParametersEnabled(), "tree parameters tab should start disabled");
        assertTrue(!state.rerootTreeEnabled(), "reroot tree tab should start disabled");
        assertTrue(!state.treeBuildEnabled(), "tree build tab should start disabled");
        assertTrue(!state.tanglegramEnabled(), "tanglegram tab should start disabled");
        assertTrue(!state.visLaunchingEnabled(), "vis launching tab should start disabled");

        state = state.markInputConfigured();
        assertTrue(state.treeParametersEnabled(), "tree parameters tab should unlock when input is configured");
        assertTrue(state.rerootTreeEnabled(), "reroot tree tab should unlock when input is configured");
        assertTrue(state.treeBuildEnabled(), "tree build tab should unlock when input is configured");
        assertTrue(!state.tanglegramEnabled(), "tanglegram tab should still be locked");
        assertTrue(!state.visLaunchingEnabled(), "vis launching tab should still be locked");

        state = state.markRunStarted();
        state = state.markTanglegramReady();
        assertTrue(state.tanglegramEnabled(), "tanglegram tab should unlock when results are ready");
        assertTrue(state.visLaunchingEnabled(), "vis launching tab should unlock when results are ready");
    }

    private static void relocksTanglegramWhenNewRunStarts() {
        WorkflowTabsState state = WorkflowTabsState.initial()
                .markInputConfigured()
                .markTanglegramReady();
        assertTrue(state.tanglegramEnabled(), "tanglegram should be enabled after a successful run");

        state = state.markRunStarted();
        assertTrue(!state.tanglegramEnabled(), "starting a new run should relock tanglegram until new results exist");
        assertTrue(!state.visLaunchingEnabled(), "starting a new run should relock vis launching until new results exist");
    }

    private static void buildsStandaloneTanglegramLaunchCommand() {
        Path scriptDir = Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1");
        Path outputDir = Paths.get("/data/output/demo");
        List<String> command = VisLaunchingPanel.buildLaunchCommand(scriptDir, outputDir, ":");
        assertEquals(
                Arrays.asList(
                        "java",
                        "-cp",
                        scriptDir.resolve("java_tanglegram") + ":" + scriptDir.resolve("lib") + java.io.File.separator + "*",
                        "tanglegram.launcher",
                        "-dir",
                        outputDir.resolve("tree_summary").toString()),
                command,
                "unexpected standalone tanglegram launch command");
        assertEquals(
                scriptDir.resolve("how_to_cite.md").toAbsolutePath().normalize(),
                HowToCitePanel.resolveCitationDocument(scriptDir),
                "unexpected citation document path");

        HowToCitePanel panel = new HowToCitePanel(scriptDir);
        assertEquals(
                scriptDir.resolve("how_to_cite.md").toAbsolutePath().normalize(),
                panel.citationDocumentPathForTest(),
                "unexpected panel citation path");
    }

    private static void buildsLinuxWorkbenchShell() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            OneBuilderWorkspacePanel workspacePanel = new OneBuilderWorkspacePanel(
                    Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1"),
                    PlatformSupport.LINUX);
            assertEquals(
                    Arrays.asList("Input / Align", "Tree Parameters", "Reroot Tree", "Tree Build", "Tanglegram", "Vis. Launching", "How to cite"),
                    workspacePanel.navigationLabels(),
                    "unexpected left navigation labels");
            assertEquals("Input / Align", workspacePanel.selectedSectionLabel(), "unexpected initial section");
            assertTrue(!workspacePanel.isSectionEnabled("Tree Parameters"), "tree parameters section should start disabled");
            assertTrue(!workspacePanel.isSectionEnabled("Reroot Tree"), "reroot tree section should start disabled");
            assertTrue(!workspacePanel.isSectionEnabled("Tree Build"), "tree build section should start disabled");
            assertTrue(!workspacePanel.isSectionEnabled("Tanglegram"), "tanglegram section should start disabled");
            assertTrue(!workspacePanel.isSectionEnabled("Vis. Launching"), "vis launching section should start disabled");
            assertTrue(workspacePanel.isSectionEnabled("How to cite"), "how to cite should always be enabled");
            assertEquals(
                    Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1/how_to_cite.md").toAbsolutePath().normalize(),
                    workspacePanel.howToCitePanel().citationDocumentPathForTest(),
                    "unexpected workspace citation path");
            assertTrue(workspacePanel.hasHeaderPanel(), "expected top header");
            assertTrue(workspacePanel.inputAlignPanel().isRunSupported(), "linux should support run");
            assertTrue(workspacePanel.inputAlignPanel().isExportSelected(), "export should default to selected");
            assertEquals(
                    Arrays.asList("Distance Method", "Maximum Likelihood", "Bayes Method", "Maximum Parsimony", "Protein Structure"),
                    workspacePanel.treeParametersPanel().parameterTreeLabels(),
                    "unexpected parameter tree labels");
            assertTrue(workspacePanel.treeParametersPanel().isProteinStructureEnabledForTest(), "protein structure should be available for protein projects");
            assertEquals(RerootMethod.MAD, workspacePanel.rerootTreePanel().toConfig().method(), "MAD should be the default reroot method");
            assertTrue(workspacePanel.treeBuildPanel().hasRunButtonForTest(), "expected tree build run button");
            assertTrue(workspacePanel.treeBuildPanel().hasExportConfigButtonForTest(), "expected tree build export button");
            assertEquals(Arrays.asList("Setup"), workspacePanel.headerRightLabelsForTest(),
                    "initial header should only show run-state status");
        });
    }

    private static void buildsWindowsWorkbenchShell() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            OneBuilderWorkspacePanel workspacePanel = new OneBuilderWorkspacePanel(
                    Paths.get("C:/onebuilder/phylotree_builder_v0.0.1"),
                    PlatformSupport.WINDOWS);
            assertTrue(!workspacePanel.inputAlignPanel().isRunSupported(), "windows should disable run");
            assertTrue(workspacePanel.treeBuildPanel().isExportConfigButtonEnabledForTest(), "windows should allow export config");
            assertTrue(!workspacePanel.isSectionEnabled("Tanglegram"), "windows should keep tanglegram disabled");
        });
    }

    private static void blocksNavigationUntilRequiredInputIsReady() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            OneBuilderWorkspacePanel workspacePanel = new OneBuilderWorkspacePanel(
                    Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1"),
                    PlatformSupport.LINUX);
            workspacePanel.clickNavigationSection("Tree Parameters");
            assertEquals("Input / Align", workspacePanel.selectedSectionLabel(), "navigation should stay on the current tab");
            assertTrue(
                    workspacePanel.navigationTooltipText("Tree Parameters").contains("Finish the required fields in Input / Align first"),
                    "expected tooltip to explain why Tree Parameters is locked");
        });
    }

    private static void remembersInputAlignBrowseDirectories() {
        UiPreferenceStore.useTestNode("/egps-onebuilder/tests/onebuilder/browse-memory");
        UiPreferenceStore.clearNodeForTests();
        UiPreferenceStore.saveRecentOneBuilderInputDir(Paths.get("/tmp/onebuilder-input-cache"));
        UiPreferenceStore.saveRecentOneBuilderOutputDir(Paths.get("/tmp/onebuilder-output-cache"));

        InputAlignPanel panel = new InputAlignPanel(PlatformSupport.LINUX, () -> { }, () -> PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN));
        assertEquals(
                Paths.get("/tmp/onebuilder-input-cache").toAbsolutePath().normalize(),
                panel.initialInputChooserPathForTest(),
                "unexpected remembered input browse directory");
        assertEquals(
                Paths.get("/tmp/onebuilder-output-cache").toAbsolutePath().normalize(),
                panel.initialOutputChooserPathForTest(),
                "unexpected remembered output browse directory");

        UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
    }

    private static void prefersInputParentDirectoryWhenInputPathIsAFile() throws Exception {
        Path tempDirectory = Files.createTempDirectory("onebuilder-input-parent-");
        Path tempInputFile = Files.createTempFile(tempDirectory, "demo-", ".fasta");

        InputAlignPanel panel = new InputAlignPanel(PlatformSupport.LINUX, () -> { }, () -> PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN));
        panel.setInputFilePathForTest(tempInputFile.toString());

        assertEquals(
                tempDirectory.toAbsolutePath().normalize(),
                panel.initialInputChooserPathForTest(),
                "expected input chooser to start from the parent directory when the current input is a file");
    }

    private static void rejectsInvalidOutputDirectoryPath() throws Exception {
        Path tempInputFile = Files.createTempFile("onebuilder-input-", ".fasta");
        InputAlignPanel panel = new InputAlignPanel(PlatformSupport.LINUX, () -> { }, () -> PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN));
        setTextField(panel, "inputFileField", tempInputFile.toString());
        setTextField(panel, "outputDirField", "bad\u0000path");

        try {
            panel.buildRunRequestForExport();
            throw new AssertionError("expected invalid output path to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Output"), "expected output-path validation message");
        }
    }

    private static void rejectsRunWhenAllTreeMethodsAreDisabled() throws Exception {
        Path tempInputFile = Files.createTempFile("onebuilder-input-", ".fasta");
        Path tempOutputDirectory = Files.createTempDirectory("onebuilder-output-");
        Files.writeString(tempInputFile, ">seq1\nMPEPTIDE\n>seq2\nMPEPTIDK\n", StandardCharsets.UTF_8);
        PipelineRuntimeConfig runtimeConfig = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN)
                .withDistance(new SimpleMethodConfig(false))
                .withMaximumLikelihood(new MaximumLikelihoodConfig(false, 1000, "MFP", ""))
                .withBayesian(new BayesianConfig(false, "mixed", "invgamma", 50000, 100, 1000, 5000))
                .withParsimony(new SimpleMethodConfig(false))
                .withProteinStructure(ProteinStructureConfig.defaults());
        InputAlignPanel panel = new InputAlignPanel(PlatformSupport.LINUX, () -> { }, () -> runtimeConfig);
        setTextField(panel, "inputFileField", tempInputFile.toString());
        setTextField(panel, "outputDirField", tempOutputDirectory.toString());

        try {
            panel.buildRunRequestForExport();
            throw new AssertionError("expected all-disabled method configuration to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Enable at least one tree-building method"),
                    "expected all-methods-disabled validation message");
        }
    }

    private static void countsEnabledTreeMethodsForSingleMethodWarning() {
        PipelineRuntimeConfig distanceOnly = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN)
                .withDistance(new SimpleMethodConfig(true))
                .withMaximumLikelihood(new MaximumLikelihoodConfig(false, 1000, "MFP", ""))
                .withBayesian(new BayesianConfig(false, "mixed", "invgamma", 50000, 100, 1000, 5000))
                .withParsimony(new SimpleMethodConfig(false))
                .withProteinStructure(ProteinStructureConfig.defaults());
        assertEquals(Integer.valueOf(1), Integer.valueOf(InputAlignPanel.countEnabledTreeMethods(distanceOnly, InputType.PROTEIN)),
                "expected exactly one enabled tree method");

        PipelineRuntimeConfig proteinStructureOnly = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN)
                .withDistance(new SimpleMethodConfig(false))
                .withMaximumLikelihood(new MaximumLikelihoodConfig(false, 1000, "MFP", ""))
                .withBayesian(new BayesianConfig(false, "mixed", "invgamma", 50000, 100, 1000, 5000))
                .withParsimony(new SimpleMethodConfig(false))
                .withProteinStructure(new ProteinStructureConfig(true, true, "/data/structures.tsv"));
        assertEquals(Integer.valueOf(1), Integer.valueOf(InputAlignPanel.countEnabledTreeMethods(proteinStructureOnly, InputType.PROTEIN)),
                "protein structure should count as one protein tree method");
        assertEquals(Integer.valueOf(0), Integer.valueOf(InputAlignPanel.countEnabledTreeMethods(proteinStructureOnly, InputType.DNA_CDS)),
                "protein structure should not count for DNA/CDS input");

        PipelineRuntimeConfig defaultProtein = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN);
        assertEquals(Integer.valueOf(4), Integer.valueOf(InputAlignPanel.countEnabledTreeMethods(defaultProtein, InputType.PROTEIN)),
                "expected default protein sequence methods to count as four enabled methods");
    }

    private static void requiresProstt5ModelPathForFastaOnlyProteinStructure() throws Exception {
        Path tempInputFile = Files.createTempFile("onebuilder-protein-input-", ".fasta");
        Path tempOutputDirectory = Files.createTempDirectory("onebuilder-protein-output-");
        Files.writeString(tempInputFile, ">seq1\nMPEPTIDE\n>seq2\nMPEPTIDK\n", StandardCharsets.UTF_8);

        PipelineRuntimeConfig runtimeConfig = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN)
                .withProteinStructure(new ProteinStructureConfig(true, false, null, null, "NJ"));
        InputAlignPanel panel = new InputAlignPanel(PlatformSupport.LINUX, () -> { }, () -> runtimeConfig);
        setTextField(panel, "inputFileField", tempInputFile.toString());
        setTextField(panel, "outputDirField", tempOutputDirectory.toString());

        try {
            panel.buildRunRequestForExport();
            throw new AssertionError("expected FASTA-only Protein Structure to require a local ProstT5 model path");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ProstT5 model weights path is required"),
                    "expected explicit ProstT5 model validation message");
        }
    }

    private static void autoDetectsDnaInputTypeForExportedConfig() throws Exception {
        Path tempInputFile = Files.createTempFile("onebuilder-dna-input-", ".fasta");
        Path tempOutputDirectory = Files.createTempDirectory("onebuilder-dna-output-");
        Path tempConfigFile = Files.createTempFile("onebuilder-dna-config-", ".json");
        Files.writeString(tempInputFile, ">seq1\nATGCTANNN---ATGC\n>seq2\nATGCTARCGTATATGC\n", StandardCharsets.UTF_8);

        final InputAlignPanel[] panelRef = new InputAlignPanel[1];
        SwingUtilities.invokeAndWait(() -> panelRef[0] = new InputAlignPanel(
                PlatformSupport.LINUX,
                () -> { },
                () -> PipelineRuntimeConfig.defaultsFor(panelRef[0] == null ? InputType.PROTEIN : panelRef[0].selectedInputType())));

        final RunRequest[] requestRef = new RunRequest[1];
        SwingUtilities.invokeAndWait(() -> {
            InputAlignPanel panel = panelRef[0];
            setTextFieldUnchecked(panel, "inputFileField", tempInputFile.toString());
            setTextFieldUnchecked(panel, "outputDirField", tempOutputDirectory.toString());
            panel.autoDetectInputTypeForCurrentFileForTest();
            assertEquals(InputType.DNA_CDS, panel.selectedInputType(), "expected DNA input type to be auto-detected");
            requestRef[0] = panel.buildRunRequestForExport();
        });

        new PipelineConfigWriter().write(tempConfigFile, requestRef[0]);
        JSONObject root = new JSONObject(Files.readString(tempConfigFile, StandardCharsets.UTF_8));
        assertEquals("DNA/CDS", root.getJSONObject("run").getString("input_type"), "unexpected exported input type");
        assertTrue(root.getJSONObject("methods").getJSONObject("distance").has("dnadist"), "expected dna distance section");
        assertTrue(root.getJSONObject("methods").getJSONObject("parsimony").has("dnapars"), "expected dna parsimony section");
        assertTrue(!root.getJSONObject("methods").getJSONObject("distance").has("protdist"), "protein distance section should be absent");
    }

    private static void autoEnablesAlignmentForUnequalSequenceLengths() throws Exception {
        Path tempInputFile = Files.createTempFile("onebuilder-raw-input-", ".fasta");
        Path tempOutputDirectory = Files.createTempDirectory("onebuilder-raw-output-");
        Files.writeString(tempInputFile, ">seq1\nATGCTA\n>seq2\nATGCTAGCTA\n", StandardCharsets.UTF_8);

        final InputAlignPanel[] panelRef = new InputAlignPanel[1];
        SwingUtilities.invokeAndWait(() -> panelRef[0] = new InputAlignPanel(
                PlatformSupport.LINUX,
                () -> { },
                () -> PipelineRuntimeConfig.defaultsFor(panelRef[0] == null ? InputType.PROTEIN : panelRef[0].selectedInputType())));

        final RunRequest[] requestRef = new RunRequest[1];
        SwingUtilities.invokeAndWait(() -> {
            InputAlignPanel panel = panelRef[0];
            setTextFieldUnchecked(panel, "inputFileField", tempInputFile.toString());
            setTextFieldUnchecked(panel, "outputDirField", tempOutputDirectory.toString());
            panel.autoDetectInputTypeForCurrentFileForTest();
            assertTrue(panel.isRunAlignmentSelectedForTest(), "unequal sequence lengths should enable alignment automatically");
            requestRef[0] = panel.buildRunRequestForExport();
        });

        assertTrue(requestRef[0].runAlignmentFirst(), "exported request should preserve auto-enabled alignment");
    }

    private static void roundTripsAdvancedRuntimeConfigThroughPanels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TreeParametersPanel proteinPanel = new TreeParametersPanel(InputType.PROTEIN);
            PipelineRuntimeConfig proteinConfig = new PipelineRuntimeConfig(
                    InputType.PROTEIN,
                    new SimpleMethodConfig(
                            true,
                            "F84",
                            Double.valueOf(2.0d),
                            true,
                            "UPGMA",
                            null,
                            null,
                            null,
                            false,
                            Arrays.asList("P", "P", "G", "0.5", "Y"),
                            java.util.Collections.emptyList(),
                            Arrays.asList("N", "Y"),
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList()),
                    new MaximumLikelihoodConfig(
                            true,
                            4000,
                            "LG",
                            "LG,WAG",
                            "AUTO",
                            Integer.valueOf(6),
                            Integer.valueOf(77),
                            true,
                            true,
                            false,
                            true,
                            false,
                            "16G",
                            "seq2",
                            "AA",
                            Integer.valueOf(0),
                            true,
                            Arrays.asList("-bnni", "-wbtl")),
                    new BayesianConfig(
                            true,
                            "wag",
                            "gamma",
                            150000,
                            250,
                            500,
                            5000,
                            null,
                            Integer.valueOf(3),
                            Integer.valueOf(6),
                            Double.valueOf(0.1),
                            Boolean.TRUE,
                            Double.valueOf(0.005),
                            Integer.valueOf(1000),
                            Double.valueOf(0.2),
                            Boolean.TRUE,
                            Arrays.asList("charset core = 1-300;", "prset statefreqpr=fixed(equal);")),
                    new SimpleMethodConfig(
                            true,
                            "F84",
                            Double.valueOf(2.0d),
                            true,
                            "NJ",
                            null,
                            Integer.valueOf(5),
                            null,
                            false,
                            false,
                            true,
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList(),
                            Arrays.asList("J", "5", "Y"),
                            java.util.Collections.emptyList()));

            proteinPanel.applyRuntimeConfig(proteinConfig);
            PipelineRuntimeConfig roundTrippedProtein = proteinPanel.runtimeConfig();

            assertEquals(4000, roundTrippedProtein.maximumLikelihood().bootstrapReplicates(), "unexpected ML bootstrap");
            assertEquals("AUTO", roundTrippedProtein.maximumLikelihood().threads(), "unexpected ML threads");
            assertEquals(Integer.valueOf(6), roundTrippedProtein.maximumLikelihood().threadsMax(), "unexpected ML thread cap");
            assertEquals(Integer.valueOf(0), roundTrippedProtein.maximumLikelihood().alrt(), "expected alrt=0 to round-trip");
            assertEquals(Arrays.asList("-bnni", "-wbtl"), roundTrippedProtein.maximumLikelihood().extraArgs(), "unexpected ML extra args");
            assertEquals(Integer.valueOf(3), roundTrippedProtein.bayesian().nruns(), "unexpected MrBayes nruns");
            assertEquals(Double.valueOf(0.2), roundTrippedProtein.bayesian().burninfrac(), "unexpected MrBayes burnin fraction");
            assertEquals(
                    Arrays.asList("charset core = 1-300;", "prset statefreqpr=fixed(equal);"),
                    roundTrippedProtein.bayesian().commandBlock(),
                    "unexpected MrBayes command block");
            assertEquals(
                    Arrays.asList("P", "P", "G", "0.5", "Y"),
                    roundTrippedProtein.distance().protdistMenuOverrides(),
                    "unexpected protdist menu overrides");
            assertEquals("UPGMA", roundTrippedProtein.distance().neighborMethod(), "unexpected protein neighbor method");
            assertNull(roundTrippedProtein.distance().neighborOutgroupIndex(), "protein UPGMA should not keep outgroup");
            assertEquals(Integer.valueOf(5), roundTrippedProtein.parsimony().protparsOutgroupIndex(),
                    "unexpected protpars outgroup");
            assertTrue(!roundTrippedProtein.parsimony().protparsPrintSteps(),
                    "expected protpars print-steps flag to round-trip");
            assertTrue(roundTrippedProtein.parsimony().protparsPrintSequences(),
                    "expected protpars print-sequences flag to round-trip");
            assertEquals(Arrays.asList("J", "5", "Y"), roundTrippedProtein.parsimony().protparsMenuOverrides(),
                    "unexpected protpars menu overrides");

            TreeParametersPanel dnaPanel = new TreeParametersPanel(InputType.DNA_CDS);
            PipelineRuntimeConfig dnaConfig = new PipelineRuntimeConfig(
                    InputType.DNA_CDS,
                    new SimpleMethodConfig(
                            true,
                            "Kimura",
                            Double.valueOf(3.5d),
                            false,
                            "NJ",
                            Integer.valueOf(7),
                            null,
                            null,
                            false,
                            java.util.Collections.emptyList(),
                            Arrays.asList("D", "T", "3.5", "F", "Y"),
                            Arrays.asList("O", "7", "Y"),
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList()),
                    new MaximumLikelihoodConfig(
                            true,
                            1500,
                            "GTR",
                            "",
                            null,
                            null,
                            null,
                            false,
                            false,
                            true,
                            false,
                            true,
                            null,
                            null,
                            null,
                            Integer.valueOf(0),
                            false,
                            java.util.Collections.emptyList()),
                    new BayesianConfig(true, null, "gamma", 25000, 100, 500, 2000, Integer.valueOf(2)),
                    new SimpleMethodConfig(
                            true,
                            "F84",
                            Double.valueOf(2.0d),
                            true,
                            "NJ",
                            null,
                            null,
                            Integer.valueOf(4),
                            true,
                            true,
                            true,
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList(),
                            java.util.Collections.emptyList(),
                            Arrays.asList("O", "4", "N", "Y")));

            dnaPanel.applyRuntimeConfig(dnaConfig);
            PipelineRuntimeConfig roundTrippedDna = dnaPanel.runtimeConfig();

            assertEquals("Kimura", roundTrippedDna.distance().dnadistModel(), "unexpected dnadist model");
            assertEquals(Double.valueOf(3.5d), roundTrippedDna.distance().dnadistTransitionTransversionRatio(),
                    "unexpected dnadist ratio");
            assertTrue(!roundTrippedDna.distance().dnadistEmpiricalBaseFrequencies(),
                    "expected empirical base frequencies to round-trip");
            assertEquals("NJ", roundTrippedDna.distance().neighborMethod(), "unexpected dna neighbor method");
            assertEquals(Integer.valueOf(7), roundTrippedDna.distance().neighborOutgroupIndex(),
                    "unexpected dna neighbor outgroup");
            assertEquals(Integer.valueOf(4), roundTrippedDna.parsimony().dnaparsOutgroupIndex(),
                    "unexpected dnapars outgroup");
            assertTrue(roundTrippedDna.parsimony().dnaparsTransversionParsimony(),
                    "expected dnapars transversion flag");
        });
    }

    private static void adaptsMaximumLikelihoodStrategiesByInputType() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MaximumLikelihoodPanel panel = new MaximumLikelihoodPanel();
            panel.setInputType(InputType.PROTEIN);
            assertEquals(
                    Arrays.asList("MFP", "TEST", "LG", "WAG", "JTT"),
                    panel.modelStrategyOptionsForTest(),
                    "unexpected protein ML strategies");
            assertTrue(panel.isModelSetEnabledForTest(), "protein model set should be enabled for MFP");

            panel.setModelStrategyForTest("LG");
            assertTrue(!panel.isModelSetEnabledForTest(), "fixed protein model should disable model-set restriction");

            panel.setInputType(InputType.DNA_CDS);
            assertEquals(
                    Arrays.asList("MFP", "TEST", "GTR", "HKY", "JC"),
                    panel.modelStrategyOptionsForTest(),
                    "unexpected DNA ML strategies");
            assertTrue(panel.isModelSetEnabledForTest(), "dna model set should be enabled for model-selection strategies");

            panel.setModelStrategyForTest("GTR");
            assertTrue(!panel.isModelSetEnabledForTest(), "fixed DNA model should disable model-set restriction");
        });
    }

    private static void showsBootstrapGuidanceForTypicalAndExtremeValues() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MaximumLikelihoodPanel panel = new MaximumLikelihoodPanel();

            assertTrue(panel.bootstrapGuidanceTextForTest().contains("1000"),
                    "default bootstrap guidance should mention the recommended default");

            panel.setBootstrapReplicatesForTest(0);
            assertTrue(panel.bootstrapGuidanceTextForTest().contains("skip"),
                    "bootstrap=0 guidance should explain that -bb is skipped");

            panel.setBootstrapReplicatesForTest(25000);
            assertTrue(panel.bootstrapGuidanceTextForTest().contains("runtime"),
                    "large bootstrap guidance should warn about runtime cost");

            panel.setAlrtEnabledForTest(true);
            assertTrue(panel.bootstrapGuidanceTextForTest().contains("-alrt"),
                    "guidance should explain the relationship between bootstrap and -alrt");
        });
    }

    private static void preservesAlrtZeroInSerializedConfig() throws Exception {
        Path tempFile = Files.createTempFile("onebuilder-config-alrt-", ".json");
        PipelineRuntimeConfig config = PipelineRuntimeConfig.defaultsFor(InputType.DNA_CDS)
                .withMaximumLikelihood(new MaximumLikelihoodConfig(
                        true,
                        1000,
                        "MFP",
                        "",
                        null,
                        null,
                        null,
                        false,
                        false,
                        true,
                        false,
                        true,
                        null,
                        null,
                        null,
                        Integer.valueOf(0),
                        false,
                        java.util.Collections.emptyList()));

        RunRequest request = RunRequest.builder()
                .inputType(InputType.DNA_CDS)
                .inputFile(Paths.get("/data/input/aligned.fasta"))
                .outputDirectory(Paths.get("/data/output"))
                .outputPrefix("dna_demo")
                .exportConfigFile(true)
                .runAlignmentFirst(false)
                .alignOptions(AlignmentOptions.defaults())
                .runtimeConfig(config)
                .build();

        new PipelineConfigWriter().write(tempFile, request);
        JSONObject advanced = new JSONObject(Files.readString(tempFile, StandardCharsets.UTF_8))
                .getJSONObject("methods")
                .getJSONObject("maximum_likelihood")
                .getJSONObject("iqtree")
                .getJSONObject("advanced");

        assertEquals(Integer.valueOf(0), Integer.valueOf(advanced.getInt("alrt")), "expected alrt=0 to be serialized");
    }

    private static void omitsStopvalWhenStopruleIsDisabled() throws Exception {
        Path tempFile = Files.createTempFile("onebuilder-config-stoprule-", ".json");
        PipelineRuntimeConfig config = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN)
                .withBayesian(new BayesianConfig(
                        true,
                        "mixed",
                        "gamma",
                        75000,
                        250,
                        1000,
                        3000,
                        null,
                        Integer.valueOf(2),
                        Integer.valueOf(4),
                        Double.valueOf(0.2),
                        Boolean.FALSE,
                        Double.valueOf(0.01),
                        Integer.valueOf(500),
                        Double.valueOf(0.25),
                        Boolean.TRUE,
                        java.util.Collections.emptyList()));

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
        JSONObject advanced = new JSONObject(Files.readString(tempFile, StandardCharsets.UTF_8))
                .getJSONObject("methods")
                .getJSONObject("bayesian")
                .getJSONObject("mrbayes")
                .getJSONObject("advanced");

        assertTrue(advanced.has("stoprule"), "expected explicit stoprule field");
        assertTrue(!advanced.has("stopval"), "stopval should be omitted when stoprule is disabled");
    }

    private static void usesPreferenceDefaultForEmbeddedTanglegramLabelSize() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        UiPreferenceStore.useTestNode("/egps-onebuilder/tests/onebuilder/preferences");
        UiPreferenceStore.clearNodeForTests();
        UiPreferenceStore.captureLookAndFeelDefaults();
        UiPreferenceStore.save(new UiPreferences("Dialog", 15, true, 22, true));

        SwingUtilities.invokeAndWait(() -> {
            CurrentRunTanglegramPanel panel = new CurrentRunTanglegramPanel();
            assertEquals(Integer.valueOf(22), Integer.valueOf(panel.labelFontSizeValueForTest()),
                    "unexpected embedded tanglegram default label size");
        });

        UiPreferenceStore.useTestNode(SUITE_PREFERENCE_NODE);
    }

    private static void loadsCurrentRunTanglegramTabs() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        Path repoRoot = findRepoRoot();
        Path sampleOutput = repoRoot.resolve("test1");
        CurrentRunTanglegramPanel[] panelRef = new CurrentRunTanglegramPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            CurrentRunTanglegramPanel panel = new CurrentRunTanglegramPanel();
            panelRef[0] = panel;
            panel.loadRunResults(sampleOutput);
        });
        assertTrue(panelRef[0].waitForLoadCompletionForTest(10000), "timed out waiting for tanglegram tabs to load");
        SwingUtilities.invokeAndWait(() -> {
            CurrentRunTanglegramPanel panel = panelRef[0];
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
        assertEquals(
                MethodProgressEvent.skipped(TreeMethodKey.PARSIMONY),
                interpreter.interpret(InputType.PROTEIN, "====Parsimony method skipped by runtime config===="),
                "unexpected skipped event");
        assertEquals(
                MethodProgressEvent.failed(TreeMethodKey.BAYESIAN),
                interpreter.interpret(InputType.DNA_CDS, "====贝叶斯法失败================"),
                "unexpected failed event");
        assertNull(
                interpreter.interpret(InputType.PROTEIN, "2026-04-12 INFO nothing to map"),
                "unexpected event for unrelated log output");
    }

    private static void keepsTreeBuildCaretAtEndAfterAppendingLogs() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            TreeBuildPanel panel = new TreeBuildPanel(PlatformSupport.LINUX, () -> { }, () -> { }, () -> { });
            panel.appendLog("first line");
            panel.appendLog("second line");
            assertEquals(Integer.valueOf(panel.documentLengthForTest()), Integer.valueOf(panel.caretPositionForTest()),
                    "caret should stay at the end of the log area");
        });
    }

    private static void tracksTreeBuildOverallProgressFromEnabledMethodsAndLogs() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            TreeBuildPanel panel = new TreeBuildPanel(PlatformSupport.LINUX, () -> { }, () -> { }, () -> { });
            PipelineRuntimeConfig runtimeConfig = new PipelineRuntimeConfig(
                    InputType.DNA_CDS,
                    new SimpleMethodConfig(true),
                    new MaximumLikelihoodConfig(
                            true,
                            1000,
                            "MFP",
                            "",
                            null,
                            null,
                            null,
                            false,
                            false,
                            true,
                            false,
                            true,
                            null,
                            null,
                            null,
                            null,
                            false,
                            List.of()),
                    new BayesianConfig(false, null, "invgamma", 10000, 100, 100, 1000, Integer.valueOf(6)),
                    new SimpleMethodConfig(true));

            panel.configureOverallProgress(runtimeConfig);
            panel.resetForRun(null);
            assertEquals(Integer.valueOf(9), Integer.valueOf(panel.overallProgressMaximumForTest()),
                    "unexpected total progress step count");
            assertTrue(panel.overallProgressIndeterminateForTest(),
                    "main progress bar should use startup animation before measurable work begins");
            assertEquals("Preparing run", panel.overallProgressTextForTest(), "unexpected initial progress text");

            panel.setMethodStatus(TreeMethodKey.DISTANCE, "Running");
            assertTrue(!panel.overallProgressIndeterminateForTest(),
                    "main progress bar should become determinate after the first measurable method event");
            assertEquals("Running Distance Method (0/9)", panel.overallProgressTextForTest(),
                    "unexpected distance running text");
            panel.setMethodStatus(TreeMethodKey.DISTANCE, "Completed");
            panel.setMethodStatus(TreeMethodKey.MAXIMUM_LIKELIHOOD, "Completed");
            panel.setMethodStatus(TreeMethodKey.BAYESIAN, "Skipped");
            panel.setMethodStatus(TreeMethodKey.PARSIMONY, "Completed");
            assertEquals(Integer.valueOf(3), Integer.valueOf(panel.overallProgressValueForTest()),
                    "disabled bayesian method should not count toward completed progress");

            panel.notePipelineOutput("2026-04-23 INFO Rerooting completed: /tmp/tree.nwk\n");
            panel.notePipelineOutput("2026-04-23 INFO Restored names in tree: /tmp/tree.nwk\n");
            panel.notePipelineOutput("2026-04-23 INFO Generating tree visualizations...\n");
            assertEquals("Generating tree visualizations (5/9)", panel.overallProgressTextForTest(),
                    "unexpected visualization progress text");
            panel.notePipelineOutput("2026-04-23 INFO Visualization completed\n");
            panel.notePipelineOutput("2026-04-23 INFO ['Rscript', '/tmp/cal_pair_wise_tree_dist.R', '/tmp/tree_meta_data.tsv']\n");
            assertTrue(panel.waitIndicatorRunningForTest(),
                    "tree distance calculation should show a wait indicator while running");
            assertEquals("Calculating tree distances, please wait... (6/9)", panel.overallProgressTextForTest(),
                    "unexpected tree distance progress text");
            panel.notePipelineOutput("2026-04-23 INFO Tree distance calculation completed\n");
            assertTrue(!panel.waitIndicatorRunningForTest(),
                    "tree distance wait indicator should stop when the step completes");
            panel.notePipelineOutput("2026-04-23 INFO Tree distance heatmaps saved: /tmp/a.png and /tmp/a.pdf\n");
            panel.notePipelineOutput("2026-04-23 INFO Summary saved: /tmp/analysis_summary.txt\n");

            assertEquals(Integer.valueOf(9), Integer.valueOf(panel.overallProgressValueForTest()),
                    "unexpected completed progress value");
            assertEquals("Finalizing analysis summary (9/9)", panel.overallProgressTextForTest(),
                    "unexpected final progress text before completion");

            panel.finishRun("Completed");
            assertEquals("Completed (9/9)", panel.overallProgressTextForTest(),
                    "unexpected completed progress text");

            TreeBuildPanel proteinPanel = new TreeBuildPanel(PlatformSupport.LINUX, () -> { }, () -> { }, () -> { });
            PipelineRuntimeConfig proteinRuntimeConfig = PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN)
                    .withProteinStructure(new ProteinStructureConfig(true, false, null, "SwiftNJ"));
            proteinPanel.configureOverallProgress(proteinRuntimeConfig);
            proteinPanel.resetForRun(null);
            assertEquals(Integer.valueOf(11), Integer.valueOf(proteinPanel.overallProgressMaximumForTest()),
                    "protein structure should add one measurable progress step");
            assertTrue(proteinPanel.overallProgressIndeterminateForTest(),
                    "protein run should also start with the startup animation");

            proteinPanel.setMethodStatus(TreeMethodKey.PROTEIN_STRUCTURE, "Running");
            assertTrue(!proteinPanel.overallProgressIndeterminateForTest(),
                    "protein structure progress event should switch the bar back to determinate mode");
            assertTrue(proteinPanel.waitIndicatorRunningForTest(),
                    "protein structure should show a wait indicator while running");
            assertEquals("Running Protein Structure, please wait... (0/11)", proteinPanel.overallProgressTextForTest(),
                    "unexpected protein structure running text");
            proteinPanel.setMethodStatus(TreeMethodKey.PROTEIN_STRUCTURE, "Completed");
            assertTrue(!proteinPanel.waitIndicatorRunningForTest(),
                    "protein structure wait indicator should stop when complete");
            assertEquals(Integer.valueOf(1), Integer.valueOf(proteinPanel.overallProgressValueForTest()),
                    "completed protein structure step should advance progress once");
        });
    }

    private static void storesLanguageInExportedConfig() throws Exception {
        Path tempFile = Files.createTempFile("onebuilder-language-", ".json");
        try {
            RunRequest request = RunRequest.builder()
                    .inputType(InputType.PROTEIN)
                    .inputFile(Paths.get("/data/input/aligned.fasta"))
                    .outputDirectory(Paths.get("/data/output"))
                    .outputPrefix("protein_demo")
                    .language(UiLanguage.CHINESE)
                    .exportConfigFile(true)
                    .runAlignmentFirst(false)
                    .alignOptions(AlignmentOptions.defaults())
                    .runtimeConfig(PipelineRuntimeConfig.defaultsFor(InputType.PROTEIN))
                    .build();

            new PipelineConfigWriter().write(tempFile, request);
            JSONObject run = new JSONObject(Files.readString(tempFile, StandardCharsets.UTF_8)).getJSONObject("run");
            assertEquals("chinese", run.getString("language"), "expected language in exported run section");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void usesGlobalFontForCitationText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            HowToCitePanel panel = new HowToCitePanel(Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1"));
            java.awt.Font textAreaFont = javax.swing.UIManager.getFont("TextArea.font");
            if (textAreaFont != null) {
                assertEquals(textAreaFont.getFamily(), panel.citationFontFamilyForTest(),
                        "citation text should use the global TextArea font family");
            }
        });
    }

    private static void showsTanglegramSnapshotChipBeforeStatus() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                OneBuilderWorkspacePanel workspacePanel = new OneBuilderWorkspacePanel(
                        Paths.get("/opt/onebuilder/phylotree_builder_v0.0.1"),
                        PlatformSupport.LINUX);
                java.lang.reflect.Method updateHeader = OneBuilderWorkspacePanel.class.getDeclaredMethod(
                        "updateHeaderForSection",
                        WorkspaceSection.class);
                updateHeader.setAccessible(true);
                updateHeader.invoke(workspacePanel, WorkspaceSection.TANGLEGRAM);
                assertTrue(workspacePanel.headerSnapshotChipVisibleForTest(),
                        "snapshot chip should be visible on the Tanglegram page");
                assertEquals(
                        Arrays.asList("Quick Snapshot of the Tanglegram", "Setup"),
                        workspacePanel.headerRightLabelsForTest(),
                        "snapshot chip should appear before the run-state status chip");
                updateHeader.invoke(workspacePanel, WorkspaceSection.TREE_BUILD);
                assertTrue(!workspacePanel.headerSnapshotChipVisibleForTest(),
                        "snapshot chip should be hidden outside the Tanglegram page");
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        });
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
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    private static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message + " expected null but was [" + value + "]");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void setTextField(Object target, String fieldName, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        javax.swing.JTextField textField = (javax.swing.JTextField) field.get(target);
        textField.setText(value);
    }

    private static void setTextFieldUnchecked(Object target, String fieldName, String value) {
        try {
            setTextField(target, fieldName, value);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
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
