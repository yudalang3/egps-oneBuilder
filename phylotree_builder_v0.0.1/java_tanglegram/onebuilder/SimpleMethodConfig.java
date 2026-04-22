package onebuilder;

import java.util.List;

public final class SimpleMethodConfig {
    private final boolean enabled;
    private final String dnadistModel;
    private final Double dnadistTransitionTransversionRatio;
    private final boolean dnadistEmpiricalBaseFrequencies;
    private final String neighborMethod;
    private final Integer neighborOutgroupIndex;
    private final Integer protparsOutgroupIndex;
    private final Integer dnaparsOutgroupIndex;
    private final boolean dnaparsTransversionParsimony;
    private final boolean protparsPrintSteps;
    private final boolean protparsPrintSequences;
    private final List<String> protdistMenuOverrides;
    private final List<String> dnadistMenuOverrides;
    private final List<String> neighborMenuOverrides;
    private final List<String> protparsMenuOverrides;
    private final List<String> dnaparsMenuOverrides;

    public SimpleMethodConfig(boolean enabled) {
        this(
                enabled,
                "F84",
                Double.valueOf(2.0d),
                true,
                "NJ",
                null,
                null,
                null,
                false,
                true,
                true,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    public SimpleMethodConfig(
            boolean enabled,
            List<String> protdistMenuOverrides,
            List<String> dnadistMenuOverrides,
            List<String> neighborMenuOverrides,
            List<String> protparsMenuOverrides,
            List<String> dnaparsMenuOverrides) {
        this(
                enabled,
                "F84",
                Double.valueOf(2.0d),
                true,
                "NJ",
                null,
                null,
                null,
                false,
                true,
                true,
                protdistMenuOverrides,
                dnadistMenuOverrides,
                neighborMenuOverrides,
                protparsMenuOverrides,
                dnaparsMenuOverrides);
    }

    public SimpleMethodConfig(
            boolean enabled,
            String dnadistModel,
            Double dnadistTransitionTransversionRatio,
            boolean dnadistEmpiricalBaseFrequencies,
            String neighborMethod,
            Integer neighborOutgroupIndex,
            Integer protparsOutgroupIndex,
            Integer dnaparsOutgroupIndex,
            boolean dnaparsTransversionParsimony,
            List<String> protdistMenuOverrides,
            List<String> dnadistMenuOverrides,
            List<String> neighborMenuOverrides,
            List<String> protparsMenuOverrides,
            List<String> dnaparsMenuOverrides) {
        this(
                enabled,
                dnadistModel,
                dnadistTransitionTransversionRatio,
                dnadistEmpiricalBaseFrequencies,
                neighborMethod,
                neighborOutgroupIndex,
                protparsOutgroupIndex,
                dnaparsOutgroupIndex,
                dnaparsTransversionParsimony,
                true,
                true,
                protdistMenuOverrides,
                dnadistMenuOverrides,
                neighborMenuOverrides,
                protparsMenuOverrides,
                dnaparsMenuOverrides);
    }

    public SimpleMethodConfig(
            boolean enabled,
            String dnadistModel,
            Double dnadistTransitionTransversionRatio,
            boolean dnadistEmpiricalBaseFrequencies,
            String neighborMethod,
            Integer neighborOutgroupIndex,
            Integer protparsOutgroupIndex,
            Integer dnaparsOutgroupIndex,
            boolean dnaparsTransversionParsimony,
            boolean protparsPrintSteps,
            boolean protparsPrintSequences,
            List<String> protdistMenuOverrides,
            List<String> dnadistMenuOverrides,
            List<String> neighborMenuOverrides,
            List<String> protparsMenuOverrides,
            List<String> dnaparsMenuOverrides) {
        this.enabled = enabled;
        this.dnadistModel = dnadistModel == null || dnadistModel.isBlank() ? "F84" : dnadistModel;
        this.dnadistTransitionTransversionRatio = dnadistTransitionTransversionRatio;
        this.dnadistEmpiricalBaseFrequencies = dnadistEmpiricalBaseFrequencies;
        this.neighborMethod = neighborMethod == null || neighborMethod.isBlank() ? "NJ" : neighborMethod;
        this.neighborOutgroupIndex = neighborOutgroupIndex;
        this.protparsOutgroupIndex = protparsOutgroupIndex;
        this.dnaparsOutgroupIndex = dnaparsOutgroupIndex;
        this.dnaparsTransversionParsimony = dnaparsTransversionParsimony;
        this.protparsPrintSteps = protparsPrintSteps;
        this.protparsPrintSequences = protparsPrintSequences;
        this.protdistMenuOverrides = immutableCopy(protdistMenuOverrides);
        this.dnadistMenuOverrides = immutableCopy(dnadistMenuOverrides);
        this.neighborMenuOverrides = immutableCopy(neighborMenuOverrides);
        this.protparsMenuOverrides = immutableCopy(protparsMenuOverrides);
        this.dnaparsMenuOverrides = immutableCopy(dnaparsMenuOverrides);
    }

    public boolean enabled() {
        return enabled;
    }

    public String dnadistModel() {
        return dnadistModel;
    }

    public Double dnadistTransitionTransversionRatio() {
        return dnadistTransitionTransversionRatio;
    }

    public boolean dnadistEmpiricalBaseFrequencies() {
        return dnadistEmpiricalBaseFrequencies;
    }

    public String neighborMethod() {
        return neighborMethod;
    }

    public Integer neighborOutgroupIndex() {
        return neighborOutgroupIndex;
    }

    public Integer protparsOutgroupIndex() {
        return protparsOutgroupIndex;
    }

    public Integer dnaparsOutgroupIndex() {
        return dnaparsOutgroupIndex;
    }

    public boolean dnaparsTransversionParsimony() {
        return dnaparsTransversionParsimony;
    }

    public boolean protparsPrintSteps() {
        return protparsPrintSteps;
    }

    public boolean protparsPrintSequences() {
        return protparsPrintSequences;
    }

    public List<String> protdistMenuOverrides() {
        return protdistMenuOverrides;
    }

    public List<String> dnadistMenuOverrides() {
        return dnadistMenuOverrides;
    }

    public List<String> neighborMenuOverrides() {
        return neighborMenuOverrides;
    }

    public List<String> protparsMenuOverrides() {
        return protparsMenuOverrides;
    }

    public List<String> dnaparsMenuOverrides() {
        return dnaparsMenuOverrides;
    }

    private static List<String> immutableCopy(List<String> values) {
        return List.copyOf(values == null ? List.of() : values);
    }
}
