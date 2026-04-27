package onebuilder;

public final class PipelineRuntimeConfig {
    private final InputType inputType;
    private final SimpleMethodConfig distance;
    private final MaximumLikelihoodConfig maximumLikelihood;
    private final BayesianConfig bayesian;
    private final SimpleMethodConfig parsimony;
    private final ProteinStructureConfig proteinStructure;
    private final RerootConfig reroot;

    public PipelineRuntimeConfig(
            InputType inputType,
            SimpleMethodConfig distance,
            MaximumLikelihoodConfig maximumLikelihood,
            BayesianConfig bayesian,
            SimpleMethodConfig parsimony) {
        this(inputType, distance, maximumLikelihood, bayesian, parsimony, ProteinStructureConfig.defaults());
    }

    public PipelineRuntimeConfig(
            InputType inputType,
            SimpleMethodConfig distance,
            MaximumLikelihoodConfig maximumLikelihood,
            BayesianConfig bayesian,
            SimpleMethodConfig parsimony,
            ProteinStructureConfig proteinStructure) {
        this(inputType, distance, maximumLikelihood, bayesian, parsimony, proteinStructure, RerootConfig.defaults());
    }

    public PipelineRuntimeConfig(
            InputType inputType,
            SimpleMethodConfig distance,
            MaximumLikelihoodConfig maximumLikelihood,
            BayesianConfig bayesian,
            SimpleMethodConfig parsimony,
            ProteinStructureConfig proteinStructure,
            RerootConfig reroot) {
        this.inputType = inputType;
        this.distance = distance;
        this.maximumLikelihood = maximumLikelihood;
        this.bayesian = bayesian;
        this.parsimony = parsimony;
        this.proteinStructure = proteinStructure == null ? ProteinStructureConfig.defaults() : proteinStructure;
        this.reroot = reroot == null ? RerootConfig.defaults() : reroot;
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
                            "WAG,LG,JTT,Dayhoff,DCMut,rtREV,cpREV,VT,Blosum62,mtMam,mtArt,HIVb,HIVw",
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
                            java.util.List.of()),
                    new BayesianConfig(true, "mixed", "invgamma", 50000, 100, 1000, 5000),
                    new SimpleMethodConfig(true),
                    ProteinStructureConfig.defaults(),
                    RerootConfig.defaults());
        }
        return new PipelineRuntimeConfig(
                inputType,
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
                        java.util.List.of()),
                new BayesianConfig(true, null, "invgamma", 10000, 100, 100, 1000, Integer.valueOf(6)),
                new SimpleMethodConfig(true),
                ProteinStructureConfig.defaults(),
                RerootConfig.defaults());
    }

    public PipelineRuntimeConfig withDistance(SimpleMethodConfig distance) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony, proteinStructure, reroot);
    }

    public PipelineRuntimeConfig withMaximumLikelihood(MaximumLikelihoodConfig maximumLikelihood) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony, proteinStructure, reroot);
    }

    public PipelineRuntimeConfig withBayesian(BayesianConfig bayesian) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony, proteinStructure, reroot);
    }

    public PipelineRuntimeConfig withParsimony(SimpleMethodConfig parsimony) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony, proteinStructure, reroot);
    }

    public PipelineRuntimeConfig withProteinStructure(ProteinStructureConfig proteinStructure) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony, proteinStructure, reroot);
    }

    public PipelineRuntimeConfig withReroot(RerootConfig reroot) {
        return new PipelineRuntimeConfig(inputType, distance, maximumLikelihood, bayesian, parsimony, proteinStructure, reroot);
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

    public ProteinStructureConfig proteinStructure() {
        return proteinStructure;
    }

    public RerootConfig reroot() {
        return reroot;
    }
}
