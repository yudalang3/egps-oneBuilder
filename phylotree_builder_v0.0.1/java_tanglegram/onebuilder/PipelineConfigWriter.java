package onebuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;

public final class PipelineConfigWriter {
    public void write(Path outputFile, RunRequest request) throws IOException {
        JSONObject root = new JSONObject();
        root.put("run", buildRunSection(request));
        root.put("alignment", buildAlignmentSection(request));
        JSONObject methods = new JSONObject();
        root.put("methods", methods);

        PipelineRuntimeConfig config = request.runtimeConfig();
        methods.put("distance", new JSONObject().put("enabled", config.distance().enabled()));
        methods.put("parsimony", new JSONObject().put("enabled", config.parsimony().enabled()));

        JSONObject maximumLikelihood = new JSONObject()
                .put("enabled", config.maximumLikelihood().enabled())
                .put("bootstrap_replicates", config.maximumLikelihood().bootstrapReplicates())
                .put("model_strategy", config.maximumLikelihood().modelStrategy());
        if (config.maximumLikelihood().modelSet() != null && !config.maximumLikelihood().modelSet().trim().isEmpty()) {
            maximumLikelihood.put("model_set", config.maximumLikelihood().modelSet());
        }
        methods.put("maximum_likelihood", maximumLikelihood);

        JSONObject bayesian = new JSONObject()
                .put("enabled", config.bayesian().enabled())
                .put("rates", config.bayesian().rates())
                .put("ngen", config.bayesian().ngen())
                .put("samplefreq", config.bayesian().samplefreq())
                .put("printfreq", config.bayesian().printfreq())
                .put("diagnfreq", config.bayesian().diagnfreq());
        if (config.bayesian().proteinModelPrior() != null) {
            bayesian.put("protein_model_prior", config.bayesian().proteinModelPrior());
        }
        if (config.bayesian().nst() != null) {
            bayesian.put("nst", config.bayesian().nst().intValue());
        }
        methods.put("bayesian", bayesian);

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
                .put("reorder", request.alignOptions().reorder());
    }
}
