package onebuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class PipelineConfigReader {
    ImportedPipelineConfig read(Path configFile) throws IOException {
        try {
            JSONObject root = new JSONObject(Files.readString(configFile, StandardCharsets.UTF_8));
            JSONObject run = requireObject(root, "run");
            InputType inputType = parseInputType(requireString(run, "input_type"), "run.input_type");
            Path inputFile = parsePath(requireString(run, "input_file"), "run.input_file");
            Path outputDirectory = parsePath(requireString(run, "output_base_dir"), "run.output_base_dir");
            String outputPrefix = requireString(run, "output_prefix").trim();
            if (outputPrefix.isEmpty()) {
                throw new IllegalArgumentException("Config field run.output_prefix must not be blank.");
            }

            AlignmentImport alignmentImport = parseAlignment(optionalObject(root, "alignment"));
            TrimAlignmentConfig trimConfig = parseTrimAlignment(optionalObject(root, "trim_alignment"));
            PipelineRuntimeConfig runtimeConfig = parseRuntimeConfig(
                    inputType,
                    optionalObject(root, "methods"),
                    optionalObject(root, "reroot"));

            return new ImportedPipelineConfig(
                    inputType,
                    inputFile,
                    outputDirectory,
                    outputPrefix,
                    alignmentImport.runAlignmentFirst,
                    alignmentImport.alignOptions,
                    trimConfig,
                    runtimeConfig);
        } catch (JSONException exception) {
            throw new IllegalArgumentException("Config file is not valid oneBuilder JSON: " + exception.getMessage(), exception);
        }
    }

    private PipelineRuntimeConfig parseRuntimeConfig(InputType inputType, JSONObject methods, JSONObject rerootObject) {
        PipelineRuntimeConfig defaults = PipelineRuntimeConfig.defaultsFor(inputType);
        JSONObject safeMethods = methods == null ? new JSONObject() : methods;
        RerootConfig reroot = parseReroot(rerootObject);
        return new PipelineRuntimeConfig(
                inputType,
                parseDistance(inputType, optionalObject(safeMethods, "distance"), defaults.distance()),
                parseMaximumLikelihood(optionalObject(safeMethods, "maximum_likelihood"), defaults.maximumLikelihood()),
                parseBayesian(inputType, optionalObject(safeMethods, "bayesian"), defaults.bayesian()),
                parseParsimony(inputType, optionalObject(safeMethods, "parsimony"), defaults.parsimony()),
                parseProteinStructure(optionalObject(safeMethods, "protein_structure"), defaults.proteinStructure()),
                reroot);
    }

    private static AlignmentImport parseAlignment(JSONObject alignment) {
        AlignmentOptions defaults = AlignmentOptions.defaults();
        if (alignment == null) {
            return new AlignmentImport(false, defaults);
        }
        JSONObject mafft = optionalObject(alignment, "mafft");
        JSONObject common = mafft == null ? new JSONObject() : optionalObject(mafft, "common");
        JSONObject source = common == null ? alignment : common;
        String strategy = stringOr(source, "strategy", stringOr(alignment, "strategy", defaults.strategy()));
        if (!Arrays.asList("localpair", "genafpair", "auto", "globalpair").contains(strategy)) {
            throw new IllegalArgumentException("Config field alignment.strategy has unsupported MAFFT strategy: " + strategy);
        }
        int maxiterate = intOr(source, "maxiterate", intOr(alignment, "maxiterate", defaults.maxiterate()));
        Integer threads = integerOrNull(source, "threads", defaults.threads());
        boolean reorder = booleanOr(source, "reorder", booleanOr(alignment, "reorder", defaults.reorder()));
        List<String> extraArgs = mafft == null ? defaults.extraArgs() : stringListOrEmpty(mafft, "extra_args");
        return new AlignmentImport(
                booleanOr(alignment, "run_alignment_first", false),
                new AlignmentOptions(strategy, maxiterate, threads, reorder, extraArgs));
    }

    private static TrimAlignmentConfig parseTrimAlignment(JSONObject trimObject) {
        TrimAlignmentConfig defaults = TrimAlignmentConfig.defaults();
        if (trimObject == null) {
            return defaults;
        }
        TrimAlignmentPreset preset = defaults.preset();
        if (trimObject.has("preset") && !trimObject.isNull("preset")) {
            preset = parseTrimPreset(trimObject.getString("preset"), "trim_alignment.preset");
        }
        JSONObject trimal = optionalObject(trimObject, "trimal");
        List<String> customArgs = trimal == null ? defaults.customArgs() : stringListOrEmpty(trimal, "custom_args");
        return new TrimAlignmentConfig(
                booleanOr(trimObject, "enabled", defaults.enabled()),
                preset,
                customArgs);
    }

    private static RerootConfig parseReroot(JSONObject rerootObject) {
        RerootConfig defaults = RerootConfig.defaults();
        if (rerootObject == null) {
            return defaults;
        }
        RerootMethod method = defaults.method();
        if (rerootObject.has("method") && !rerootObject.isNull("method")) {
            method = parseRerootMethod(rerootObject.getString("method"), "reroot.method");
        }
        JSONObject ladderization = optionalObject(rerootObject, "ladderization");
        LadderizeDirection direction = defaults.ladderizeDirection();
        boolean sortByCladeSize = defaults.sortByCladeSize();
        boolean sortByLeafNameString = defaults.sortByLeafNameString();
        boolean sortByBranchLength = defaults.sortByBranchLength();
        if (ladderization != null) {
            if (ladderization.has("direction") && !ladderization.isNull("direction")) {
                direction = parseLadderizeDirection(ladderization.getString("direction"), "reroot.ladderization.direction");
            }
            sortByCladeSize = booleanOr(ladderization, "sort_by_clade_size", sortByCladeSize);
            sortByLeafNameString = booleanOr(ladderization, "sort_by_leaf_name_string", sortByLeafNameString);
            sortByBranchLength = booleanOr(ladderization, "sort_by_branch_length", sortByBranchLength);
        }
        return new RerootConfig(method, direction, sortByCladeSize, sortByLeafNameString, sortByBranchLength);
    }

    private static SimpleMethodConfig parseDistance(InputType inputType, JSONObject distanceObject, SimpleMethodConfig defaults) {
        if (distanceObject == null) {
            return defaults;
        }
        JSONObject dnadistCommon = commonObject(optionalObject(distanceObject, "dnadist"));
        JSONObject neighborCommon = commonObject(optionalObject(distanceObject, "neighbor"));
        return new SimpleMethodConfig(
                booleanOr(distanceObject, "enabled", defaults.enabled()),
                stringOr(dnadistCommon, "dnadist_model", defaults.dnadistModel()),
                doubleObjectOrNull(dnadistCommon, "dnadist_transition_transversion_ratio", defaults.dnadistTransitionTransversionRatio()),
                booleanOr(dnadistCommon, "dnadist_empirical_base_frequencies", defaults.dnadistEmpiricalBaseFrequencies()),
                normalizeNeighborMethod(stringOr(neighborCommon, "neighbor_method", defaults.neighborMethod())),
                integerOrNull(neighborCommon, "neighbor_outgroup_index", defaults.neighborOutgroupIndex()),
                defaults.protparsOutgroupIndex(),
                defaults.dnaparsOutgroupIndex(),
                defaults.dnaparsTransversionParsimony(),
                defaults.protparsPrintSteps(),
                defaults.protparsPrintSequences(),
                inputType == InputType.PROTEIN
                        ? menuOverrides(optionalObject(distanceObject, "protdist"), defaults.protdistMenuOverrides())
                        : defaults.protdistMenuOverrides(),
                inputType == InputType.DNA_CDS
                        ? menuOverrides(optionalObject(distanceObject, "dnadist"), defaults.dnadistMenuOverrides())
                        : defaults.dnadistMenuOverrides(),
                menuOverrides(optionalObject(distanceObject, "neighbor"), defaults.neighborMenuOverrides()),
                defaults.protparsMenuOverrides(),
                defaults.dnaparsMenuOverrides());
    }

    private static MaximumLikelihoodConfig parseMaximumLikelihood(JSONObject mlObject, MaximumLikelihoodConfig defaults) {
        if (mlObject == null) {
            return defaults;
        }
        JSONObject iqtree = optionalObject(mlObject, "iqtree");
        JSONObject common = commonObject(iqtree);
        JSONObject advanced = advancedObject(iqtree);
        Integer alrt = advanced != null && advanced.has("alrt") && !advanced.isNull("alrt")
                ? Integer.valueOf(advanced.getInt("alrt"))
                : defaults.alrt();
        return new MaximumLikelihoodConfig(
                booleanOr(mlObject, "enabled", defaults.enabled()),
                intOr(common, "bootstrap_replicates", defaults.bootstrapReplicates()),
                stringOr(common, "model_strategy", defaults.modelStrategy()),
                stringOr(common, "model_set", defaults.modelSet()),
                stringOrNull(advanced, "threads", defaults.threads()),
                integerOrNull(advanced, "threads_max", defaults.threadsMax()),
                integerOrNull(advanced, "seed", defaults.seed()),
                booleanOr(advanced, "safe", defaults.safe()),
                booleanOr(advanced, "keep_ident", defaults.keepIdent()),
                booleanOr(advanced, "quiet", defaults.quiet()),
                booleanOr(advanced, "verbose", defaults.verbose()),
                booleanOr(advanced, "redo", defaults.redo()),
                stringOrNull(advanced, "memory_limit", defaults.memoryLimit()),
                stringOrNull(advanced, "outgroup", defaults.outgroup()),
                stringOrNull(advanced, "sequence_type", defaults.sequenceType()),
                alrt,
                booleanOr(advanced, "abayes", defaults.abayes()),
                iqtree == null ? defaults.extraArgs() : stringListOrEmpty(iqtree, "extra_args"));
    }

    private static BayesianConfig parseBayesian(InputType inputType, JSONObject bayesianObject, BayesianConfig defaults) {
        if (bayesianObject == null) {
            return defaults;
        }
        JSONObject mrbayes = optionalObject(bayesianObject, "mrbayes");
        JSONObject common = commonObject(mrbayes);
        JSONObject advanced = advancedObject(mrbayes);
        return new BayesianConfig(
                booleanOr(bayesianObject, "enabled", defaults.enabled()),
                inputType == InputType.PROTEIN ? stringOrNull(common, "protein_model_prior", defaults.proteinModelPrior()) : null,
                stringOr(common, "rates", defaults.rates()),
                intOr(common, "ngen", defaults.ngen()),
                intOr(common, "samplefreq", defaults.samplefreq()),
                intOr(common, "printfreq", defaults.printfreq()),
                intOr(common, "diagnfreq", defaults.diagnfreq()),
                inputType == InputType.DNA_CDS ? integerOrNull(common, "nst", defaults.nst()) : null,
                integerOrNull(advanced, "nruns", defaults.nruns()),
                integerOrNull(advanced, "nchains", defaults.nchains()),
                doubleObjectOrNull(advanced, "temp", defaults.temp()),
                booleanObjectOrNull(advanced, "stoprule", defaults.stoprule()),
                doubleObjectOrNull(advanced, "stopval", defaults.stopval()),
                integerOrNull(advanced, "burnin", defaults.burnin()),
                doubleObjectOrNull(advanced, "burninfrac", defaults.burninfrac()),
                booleanObjectOrNull(advanced, "relburnin", defaults.relburnin()),
                mrbayes == null ? defaults.commandBlock() : stringListOrEmpty(mrbayes, "command_block"));
    }

    private static SimpleMethodConfig parseParsimony(InputType inputType, JSONObject parsimonyObject, SimpleMethodConfig defaults) {
        if (parsimonyObject == null) {
            return defaults;
        }
        JSONObject protparsCommon = commonObject(optionalObject(parsimonyObject, "protpars"));
        JSONObject dnaparsCommon = commonObject(optionalObject(parsimonyObject, "dnapars"));
        return new SimpleMethodConfig(
                booleanOr(parsimonyObject, "enabled", defaults.enabled()),
                defaults.dnadistModel(),
                defaults.dnadistTransitionTransversionRatio(),
                defaults.dnadistEmpiricalBaseFrequencies(),
                defaults.neighborMethod(),
                defaults.neighborOutgroupIndex(),
                integerOrNull(protparsCommon, "protpars_outgroup_index", defaults.protparsOutgroupIndex()),
                integerOrNull(dnaparsCommon, "dnapars_outgroup_index", defaults.dnaparsOutgroupIndex()),
                booleanOr(dnaparsCommon, "dnapars_transversion_parsimony", defaults.dnaparsTransversionParsimony()),
                booleanOr(protparsCommon, "protpars_print_steps", defaults.protparsPrintSteps()),
                booleanOr(protparsCommon, "protpars_print_sequences", defaults.protparsPrintSequences()),
                defaults.protdistMenuOverrides(),
                defaults.dnadistMenuOverrides(),
                defaults.neighborMenuOverrides(),
                inputType == InputType.PROTEIN
                        ? menuOverrides(optionalObject(parsimonyObject, "protpars"), defaults.protparsMenuOverrides())
                        : defaults.protparsMenuOverrides(),
                inputType == InputType.DNA_CDS
                        ? menuOverrides(optionalObject(parsimonyObject, "dnapars"), defaults.dnaparsMenuOverrides())
                        : defaults.dnaparsMenuOverrides());
    }

    private static ProteinStructureConfig parseProteinStructure(JSONObject section, ProteinStructureConfig defaults) {
        if (section == null) {
            return defaults;
        }
        JSONObject foldseek = optionalObject(section, "foldseek");
        JSONObject common = commonObject(foldseek);
        JSONObject advanced = advancedObject(foldseek);
        return new ProteinStructureConfig(
                booleanOr(section, "enabled", defaults.enabled()),
                booleanOr(section, "use_structure_manifest", defaults.useStructureManifest()),
                stringOrNull(section, "structure_manifest_file", defaults.structureManifestFile()),
                stringOrNull(section, "prostt5_model_path", defaults.prostt5ModelPath()),
                stringOr(section, "tree_builder_method", defaults.treeBuilderMethod()),
                intOr(common, "threads", defaults.threads()),
                doubleOr(common, "sensitivity", defaults.sensitivity()),
                doubleOr(common, "evalue", defaults.evalue()),
                intOr(common, "max_seqs", defaults.maxSeqs()),
                doubleOr(common, "coverage_threshold", defaults.coverageThreshold()),
                intOr(common, "coverage_mode", defaults.coverageMode()),
                intOr(common, "alignment_type", defaults.alignmentType()),
                doubleOr(advanced, "tmscore_threshold", defaults.tmscoreThreshold()),
                booleanOr(advanced, "exhaustive_search", defaults.exhaustiveSearch()),
                booleanOr(advanced, "exact_tmscore", defaults.exactTmscore()),
                booleanOr(advanced, "gpu", defaults.gpu()),
                intOr(advanced, "verbosity", defaults.verbosity()),
                foldseek == null ? defaults.extraArgs() : stringListOrEmpty(foldseek, "extra_args"));
    }

    private static InputType parseInputType(String value, String fieldName) {
        for (InputType inputType : InputType.values()) {
            if (inputType.name().equalsIgnoreCase(value) || inputType.displayName().equalsIgnoreCase(value)) {
                return inputType;
            }
        }
        throw new IllegalArgumentException("Config field " + fieldName + " has unsupported input type: " + value);
    }

    private static TrimAlignmentPreset parseTrimPreset(String value, String fieldName) {
        for (TrimAlignmentPreset preset : TrimAlignmentPreset.values()) {
            if (preset.name().equalsIgnoreCase(value)) {
                return preset;
            }
        }
        throw new IllegalArgumentException("Config field " + fieldName + " has unsupported trim preset: " + value);
    }

    private static RerootMethod parseRerootMethod(String value, String fieldName) {
        for (RerootMethod method : RerootMethod.values()) {
            if (method.jsonValue().equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Config field " + fieldName + " has unsupported reroot method: " + value);
    }

    private static LadderizeDirection parseLadderizeDirection(String value, String fieldName) {
        for (LadderizeDirection direction : LadderizeDirection.values()) {
            if (direction.jsonValue().equalsIgnoreCase(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Config field " + fieldName + " has unsupported ladderize direction: " + value);
    }

    private static JSONObject requireObject(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            throw new IllegalArgumentException("Config is missing required object: " + key);
        }
        return object.getJSONObject(key);
    }

    private static JSONObject optionalObject(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return null;
        }
        return object.getJSONObject(key);
    }

    private static JSONObject commonObject(JSONObject object) {
        return object == null ? null : optionalObject(object, "common");
    }

    private static JSONObject advancedObject(JSONObject object) {
        return object == null ? null : optionalObject(object, "advanced");
    }

    private static String requireString(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            throw new IllegalArgumentException("Config is missing required field: " + key);
        }
        String value = object.getString(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Config field " + key + " must not be blank.");
        }
        return value;
    }

    private static Path parsePath(String value, String fieldName) {
        try {
            return Paths.get(value).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Config field " + fieldName + " is not a valid path: " + value, exception);
        }
    }

    private static String stringOr(JSONObject object, String key, String defaultValue) {
        String value = stringOrNull(object, key, defaultValue);
        return value == null ? "" : value;
    }

    private static String stringOrNull(JSONObject object, String key, String defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }
        return object.getString(key);
    }

    private static boolean booleanOr(JSONObject object, String key, boolean defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }
        return object.getBoolean(key);
    }

    private static Boolean booleanObjectOrNull(JSONObject object, String key, Boolean defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }
        return Boolean.valueOf(object.getBoolean(key));
    }

    private static int intOr(JSONObject object, String key, int defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }
        return object.getInt(key);
    }

    private static Integer integerOrNull(JSONObject object, String key, Integer defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }
        return Integer.valueOf(object.getInt(key));
    }

    private static double doubleOr(JSONObject object, String key, double defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }
        return object.getDouble(key);
    }

    private static Double doubleObjectOrNull(JSONObject object, String key, Double defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }
        return Double.valueOf(object.getDouble(key));
    }

    private static List<String> stringListOrEmpty(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return List.of();
        }
        JSONArray array = object.getJSONArray(key);
        List<String> values = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            values.add(array.getString(index));
        }
        return values;
    }

    private static List<String> menuOverrides(JSONObject programObject, List<String> defaults) {
        if (programObject == null || !programObject.has("menu_overrides") || programObject.isNull("menu_overrides")) {
            return defaults;
        }
        return stringListOrEmpty(programObject, "menu_overrides");
    }

    private static String normalizeNeighborMethod(String value) {
        if (value == null) {
            return "NJ";
        }
        return "UPGMA".equalsIgnoreCase(value) ? "UPGMA" : "NJ";
    }

    private static final class AlignmentImport {
        private final boolean runAlignmentFirst;
        private final AlignmentOptions alignOptions;

        private AlignmentImport(boolean runAlignmentFirst, AlignmentOptions alignOptions) {
            this.runAlignmentFirst = runAlignmentFirst;
            this.alignOptions = alignOptions;
        }
    }
}
