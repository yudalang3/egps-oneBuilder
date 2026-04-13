package onebuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;

public final class PipelineConfigWriter {
    public void write(Path outputFile, PipelineRuntimeConfig config) throws IOException {
        JSONObject root = new JSONObject();
        JSONObject methods = new JSONObject();
        root.put("methods", methods);

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

        Files.write(outputFile, root.toString().getBytes(StandardCharsets.UTF_8));
    }
}
