package onebuilder;

import java.util.List;

public final class BayesianConfig {
    private final boolean enabled;
    private final String proteinModelPrior;
    private final String rates;
    private final int ngen;
    private final int samplefreq;
    private final int printfreq;
    private final int diagnfreq;
    private final Integer nst;
    private final Integer nruns;
    private final Integer nchains;
    private final Double temp;
    private final Boolean stoprule;
    private final Double stopval;
    private final Integer burnin;
    private final Double burninfrac;
    private final Boolean relburnin;
    private final List<String> commandBlock;

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
        this(
                enabled,
                proteinModelPrior,
                rates,
                ngen,
                samplefreq,
                printfreq,
                diagnfreq,
                nst,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of());
    }

    public BayesianConfig(
            boolean enabled,
            String proteinModelPrior,
            String rates,
            int ngen,
            int samplefreq,
            int printfreq,
            int diagnfreq,
            Integer nst,
            Integer nruns,
            Integer nchains,
            Double temp,
            Boolean stoprule,
            Double stopval,
            Integer burnin,
            Double burninfrac,
            Boolean relburnin,
            List<String> commandBlock) {
        this.enabled = enabled;
        this.proteinModelPrior = proteinModelPrior;
        this.rates = rates;
        this.ngen = ngen;
        this.samplefreq = samplefreq;
        this.printfreq = printfreq;
        this.diagnfreq = diagnfreq;
        this.nst = nst;
        this.nruns = nruns;
        this.nchains = nchains;
        this.temp = temp;
        this.stoprule = stoprule;
        this.stopval = stopval;
        this.burnin = burnin;
        this.burninfrac = burninfrac;
        this.relburnin = relburnin;
        this.commandBlock = List.copyOf(commandBlock == null ? List.of() : commandBlock);
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

    public Integer nruns() {
        return nruns;
    }

    public Integer nchains() {
        return nchains;
    }

    public Double temp() {
        return temp;
    }

    public Boolean stoprule() {
        return stoprule;
    }

    public Double stopval() {
        return stopval;
    }

    public Integer burnin() {
        return burnin;
    }

    public Double burninfrac() {
        return burninfrac;
    }

    public Boolean relburnin() {
        return relburnin;
    }

    public List<String> commandBlock() {
        return commandBlock;
    }
}
