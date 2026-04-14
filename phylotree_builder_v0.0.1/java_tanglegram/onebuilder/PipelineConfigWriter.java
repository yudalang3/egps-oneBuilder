package onebuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONArray;
import org.json.JSONObject;

public final class PipelineConfigWriter {
    public void write(Path outputFile, RunRequest request) throws IOException {
        JSONObject root = new JSONObject();
        root.put("run", buildRunSection(request));
        root.put("alignment", buildAlignmentSection(request));
        JSONObject methods = new JSONObject();
        root.put("methods", methods);

        PipelineRuntimeConfig config = request.runtimeConfig();
        methods.put("distance", buildDistanceSection(config));
        methods.put("maximum_likelihood", buildMaximumLikelihoodSection(config));
        methods.put("bayesian", buildBayesianSection(config));
        methods.put("parsimony", buildParsimonySection(config));

        Files.write(outputFile, root.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    private static JSONObject buildRunSection(RunRequest request) {
        return new JSONObject()
                .put("input_type", request.inputType().toString())
                .put("input_file", request.inputFile().toString())
                .put("output_base_dir", request.outputDirectory().toString())
                .put("output_prefix", request.outputPrefix());
    }

    private static JSONObject buildAlignmentSection(RunRequest request) {
        return new JSONObject()
                .put("run_alignment_first", request.runAlignmentFirst())
                .put("strategy", request.alignOptions().strategy())
                .put("maxiterate", request.alignOptions().maxiterate())
                .put("reorder", request.alignOptions().reorder())
                .put("mafft", new JSONObject()
                        .put("common", new JSONObject()
                                .put("strategy", request.alignOptions().strategy())
                                .put("maxiterate", request.alignOptions().maxiterate())
                                .put("reorder", request.alignOptions().reorder()))
                        .put("advanced", new JSONObject())
                        .put("extra_args", new JSONArray(request.alignOptions().extraArgs())));
    }

    private static JSONObject buildDistanceSection(PipelineRuntimeConfig config) {
        JSONObject section = new JSONObject().put("enabled", config.distance().enabled());
        if (config.inputType() == InputType.PROTEIN) {
            section.put("protdist", phylipProgramSection(config.distance().protdistMenuOverrides()));
        } else {
            section.put("dnadist", phylipProgramSection(config.distance().dnadistMenuOverrides()));
        }
        section.put("neighbor", phylipProgramSection(config.distance().neighborMenuOverrides()));
        return section;
    }

    private static JSONObject buildMaximumLikelihoodSection(PipelineRuntimeConfig config) {
        JSONObject common = new JSONObject()
                .put("bootstrap_replicates", config.maximumLikelihood().bootstrapReplicates())
                .put("model_strategy", config.maximumLikelihood().modelStrategy());
        if (config.maximumLikelihood().modelSet() != null && !config.maximumLikelihood().modelSet().trim().isEmpty()) {
            common.put("model_set", config.maximumLikelihood().modelSet());
        }
        JSONObject advanced = new JSONObject();
        putIfPresent(advanced, "threads", config.maximumLikelihood().threads());
        putIfPresent(advanced, "threads_max", config.maximumLikelihood().threadsMax());
        putIfPresent(advanced, "seed", config.maximumLikelihood().seed());
        advanced.put("safe", config.maximumLikelihood().safe());
        advanced.put("keep_ident", config.maximumLikelihood().keepIdent());
        advanced.put("quiet", config.maximumLikelihood().quiet());
        advanced.put("verbose", config.maximumLikelihood().verbose());
        advanced.put("redo", config.maximumLikelihood().redo());
        putIfPresent(advanced, "memory_limit", config.maximumLikelihood().memoryLimit());
        putIfPresent(advanced, "outgroup", config.maximumLikelihood().outgroup());
        putIfPresent(advanced, "sequence_type", config.maximumLikelihood().sequenceType());
        putIfPresent(advanced, "alrt", config.maximumLikelihood().alrt());
        advanced.put("abayes", config.maximumLikelihood().abayes());
        return new JSONObject()
                .put("enabled", config.maximumLikelihood().enabled())
                .put("iqtree", new JSONObject()
                        .put("common", common)
                        .put("advanced", advanced)
                        .put("extra_args", new JSONArray(config.maximumLikelihood().extraArgs())));
    }

    private static JSONObject buildBayesianSection(PipelineRuntimeConfig config) {
        JSONObject common = new JSONObject()
                .put("rates", config.bayesian().rates())
                .put("ngen", config.bayesian().ngen())
                .put("samplefreq", config.bayesian().samplefreq())
                .put("printfreq", config.bayesian().printfreq())
                .put("diagnfreq", config.bayesian().diagnfreq());
        if (config.bayesian().proteinModelPrior() != null) {
            common.put("protein_model_prior", config.bayesian().proteinModelPrior());
        }
        if (config.bayesian().nst() != null) {
            common.put("nst", config.bayesian().nst().intValue());
        }
        JSONObject advanced = new JSONObject();
        putIfPresent(advanced, "nruns", config.bayesian().nruns());
        putIfPresent(advanced, "nchains", config.bayesian().nchains());
        putIfPresent(advanced, "temp", config.bayesian().temp());
        putIfPresent(advanced, "stoprule", config.bayesian().stoprule());
        putIfPresent(advanced, "stopval", config.bayesian().stopval());
        putIfPresent(advanced, "burnin", config.bayesian().burnin());
        putIfPresent(advanced, "burninfrac", config.bayesian().burninfrac());
        putIfPresent(advanced, "relburnin", config.bayesian().relburnin());
        return new JSONObject()
                .put("enabled", config.bayesian().enabled())
                .put("mrbayes", new JSONObject()
                        .put("common", common)
                        .put("advanced", advanced)
                        .put("command_block", new JSONArray(config.bayesian().commandBlock())));
    }

    private static JSONObject buildParsimonySection(PipelineRuntimeConfig config) {
        JSONObject section = new JSONObject().put("enabled", config.parsimony().enabled());
        if (config.inputType() == InputType.PROTEIN) {
            section.put("protpars", phylipProgramSection(config.parsimony().protparsMenuOverrides()));
        } else {
            section.put("dnapars", phylipProgramSection(config.parsimony().dnaparsMenuOverrides()));
        }
        return section;
    }

    private static JSONObject phylipProgramSection(java.util.List<String> menuOverrides) {
        return new JSONObject()
                .put("common", new JSONObject())
                .put("advanced", new JSONObject())
                .put("menu_overrides", new JSONArray(menuOverrides));
    }

    private static void putIfPresent(JSONObject object, String key, Object value) {
        if (value != null) {
            object.put(key, value);
        }
    }
}
