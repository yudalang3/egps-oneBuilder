package onebuilder;

public final class PipelineRuntimeConfig {
    private final InputType inputType;
    private final SimpleMethodConfig distance;
    private final MaximumLikelihoodConfig maximumLikelihood;
    private final BayesianConfig bayesian;
    private final SimpleMethodConfig parsimony;

    public PipelineRuntimeConfig(
            InputType inputType,
            SimpleMethodConfig distance,
            MaximumLikelihoodConfig maximumLikelihood,
            BayesianConfig bayesian,
            SimpleMethodConfig parsimony) {
        this.inputType = inputType;
        this.distance = distance;
        this.maximumLikelihood = maximumLikelihood;
        this.bayesian = bayesian;
        this.parsimony = parsimony;
    }

    public static PipelineRuntimeConfig defaultsFor(InputType inputType) {
        if (inputType == InputType.PROTEIN) {
            return new PipelineRuntimeConfig(
                    inputType,
                    new SimpleMethodConfig(true),
                    new MaximumLikelihoodConfig(
                            true,
                            1000,
                            "MFP",
                            "WAG,LG,JTT,Dayhoff,DCMut,rtREV,cpREV,VT,Blosum62,mtMam,mtArt,HIVb,HIVw"),
                    new BayesianConfig(true, "mixed", "invgamma", 50000, 100, 1000, 5000),
                    new SimpleMethodConfig(true));
        }
        return new PipelineRuntimeConfig(
                inputType,
                new SimpleMethodConfig(true),
                new MaximumLikelihoodConfig(true, 1000, "MFP", ""),
                new BayesianConfig(true, null, "invgamma", 10000, 100, 100, 1000, Integer.valueOf(6)),
                new SimpleMethodConfig(true));
    }

    public PipelineRuntimeConfig withDistance(SimpleMethodConfig distance) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony);
    }

    public PipelineRuntimeConfig withMaximumLikelihood(MaximumLikelihoodConfig maximumLikelihood) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony);
    }

    public PipelineRuntimeConfig withBayesian(BayesianConfig bayesian) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony);
    }

    public PipelineRuntimeConfig withParsimony(SimpleMethodConfig parsimony) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony);
    }

    public InputType inputType() {
        return inputType;
    }

    public SimpleMethodConfig distance() {
        return distance;
    }

    public MaximumLikelihoodConfig maximumLikelihood() {
        return maximumLikelihood;
    }

    public BayesianConfig bayesian() {
        return bayesian;
    }

    public SimpleMethodConfig parsimony() {
        return parsimony;
    }
}
