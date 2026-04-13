package onebuilder;

public final class BayesianConfig {
    private final boolean enabled;
    private final String proteinModelPrior;
    private final String rates;
    private final int ngen;
    private final int samplefreq;
    private final int printfreq;
    private final int diagnfreq;
    private final Integer nst;

    public BayesianConfig(
            boolean enabled,
            String proteinModelPrior,
            String rates,
            int ngen,
            int samplefreq,
            int printfreq,
            int diagnfreq) {
        this(enabled, proteinModelPrior, rates, ngen, samplefreq, printfreq, diagnfreq, null);
    }

    public BayesianConfig(
            boolean enabled,
            String proteinModelPrior,
            String rates,
            int ngen,
            int samplefreq,
            int printfreq,
            int diagnfreq,
            Integer nst) {
        this.enabled = enabled;
        this.proteinModelPrior = proteinModelPrior;
        this.rates = rates;
        this.ngen = ngen;
        this.samplefreq = samplefreq;
        this.printfreq = printfreq;
        this.diagnfreq = diagnfreq;
        this.nst = nst;
    }

    public boolean enabled() {
        return enabled;
    }

    public String proteinModelPrior() {
        return proteinModelPrior;
    }

    public String rates() {
        return rates;
    }

    public int ngen() {
        return ngen;
    }

    public int samplefreq() {
        return samplefreq;
    }

    public int printfreq() {
        return printfreq;
    }

    public int diagnfreq() {
        return diagnfreq;
    }

    public Integer nst() {
        return nst;
    }
}
