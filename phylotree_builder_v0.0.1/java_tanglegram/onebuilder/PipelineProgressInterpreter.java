package onebuilder;

public final class PipelineProgressInterpreter {
    public MethodProgressEvent interpret(InputType inputType, String line) {
        if (line == null) {
            return null;
        }

        if (containsAny(line, "Starting distance-based inference", "开始距离法建树")) {
            return MethodProgressEvent.running(TreeMethodKey.DISTANCE);
        }
        if (line.contains("====Distance method complete")) {
            return MethodProgressEvent.completed(TreeMethodKey.DISTANCE);
        }
        if (line.contains("====Distance method skipped by runtime config====")) {
            return MethodProgressEvent.skipped(TreeMethodKey.DISTANCE);
        }
        if (containsAny(line, "Starting maximum likelihood inference", "开始极大似然法建树")) {
            return MethodProgressEvent.running(TreeMethodKey.MAXIMUM_LIKELIHOOD);
        }
        if (line.contains("====Maximum likelihood method complete")) {
            return MethodProgressEvent.completed(TreeMethodKey.MAXIMUM_LIKELIHOOD);
        }
        if (line.contains("====Maximum likelihood method skipped by runtime config====")) {
            return MethodProgressEvent.skipped(TreeMethodKey.MAXIMUM_LIKELIHOOD);
        }
        if (containsAny(line, "Starting Bayesian inference", "开始贝叶斯法建树")) {
            return MethodProgressEvent.running(TreeMethodKey.BAYESIAN);
        }
        if (line.contains("====Bayesian method complete")) {
            return MethodProgressEvent.completed(TreeMethodKey.BAYESIAN);
        }
        if (line.contains("====Bayesian method skipped by runtime config====")) {
            return MethodProgressEvent.skipped(TreeMethodKey.BAYESIAN);
        }
        if (containsAny(line, "Starting parsimony inference", "开始简约法建树")) {
            return MethodProgressEvent.running(TreeMethodKey.PARSIMONY);
        }
        if (line.contains("====Parsimony method complete")) {
            return MethodProgressEvent.completed(TreeMethodKey.PARSIMONY);
        }
        if (line.contains("====Parsimony method skipped by runtime config====")) {
            return MethodProgressEvent.skipped(TreeMethodKey.PARSIMONY);
        }
        return null;
    }

    private static boolean containsAny(String line, String first, String second) {
        return line.contains(first) || line.contains(second);
    }
}
