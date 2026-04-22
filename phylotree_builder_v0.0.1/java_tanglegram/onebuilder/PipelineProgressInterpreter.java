package onebuilder;

public final class PipelineProgressInterpreter {
    public MethodProgressEvent interpret(InputType inputType, String line) {
        if (line == null) {
            return null;
        }

        if (containsAny(line, "Starting distance-based inference", "开始距离法建树")) {
            return MethodProgressEvent.running(TreeMethodKey.DISTANCE);
        }
        if (containsAny(line, "====Distance method complete", "====距离法完成")) {
            return MethodProgressEvent.completed(TreeMethodKey.DISTANCE);
        }
        if (containsAny(line, "====Distance method failed", "====距离法失败")) {
            return MethodProgressEvent.failed(TreeMethodKey.DISTANCE);
        }
        if (containsAny(line, "====Distance method skipped by runtime config====", "====距离法已按运行时配置跳过====")) {
            return MethodProgressEvent.skipped(TreeMethodKey.DISTANCE);
        }
        if (containsAny(line, "Starting maximum likelihood inference", "开始极大似然法建树")) {
            return MethodProgressEvent.running(TreeMethodKey.MAXIMUM_LIKELIHOOD);
        }
        if (containsAny(line, "====Maximum likelihood method complete", "====极大似然法完成")) {
            return MethodProgressEvent.completed(TreeMethodKey.MAXIMUM_LIKELIHOOD);
        }
        if (containsAny(line, "====Maximum likelihood method failed", "====极大似然法失败")) {
            return MethodProgressEvent.failed(TreeMethodKey.MAXIMUM_LIKELIHOOD);
        }
        if (containsAny(line, "====Maximum likelihood method skipped by runtime config====", "====极大似然法已按运行时配置跳过====")) {
            return MethodProgressEvent.skipped(TreeMethodKey.MAXIMUM_LIKELIHOOD);
        }
        if (containsAny(line, "Starting Bayesian inference", "开始贝叶斯法建树")) {
            return MethodProgressEvent.running(TreeMethodKey.BAYESIAN);
        }
        if (containsAny(line, "====Bayesian method complete", "====贝叶斯法完成")) {
            return MethodProgressEvent.completed(TreeMethodKey.BAYESIAN);
        }
        if (containsAny(line, "====Bayesian method failed", "====贝叶斯法失败")) {
            return MethodProgressEvent.failed(TreeMethodKey.BAYESIAN);
        }
        if (containsAny(line, "====Bayesian method skipped by runtime config====", "====贝叶斯法已按运行时配置跳过====")) {
            return MethodProgressEvent.skipped(TreeMethodKey.BAYESIAN);
        }
        if (containsAny(line, "Starting parsimony inference", "开始简约法建树")) {
            return MethodProgressEvent.running(TreeMethodKey.PARSIMONY);
        }
        if (containsAny(line, "====Parsimony method complete", "====简约法完成")) {
            return MethodProgressEvent.completed(TreeMethodKey.PARSIMONY);
        }
        if (containsAny(line, "====Parsimony method failed", "====简约法失败")) {
            return MethodProgressEvent.failed(TreeMethodKey.PARSIMONY);
        }
        if (containsAny(line, "====Parsimony method skipped by runtime config====", "====简约法已按运行时配置跳过====")) {
            return MethodProgressEvent.skipped(TreeMethodKey.PARSIMONY);
        }
        return null;
    }

    private static boolean containsAny(String line, String first, String second) {
        return line.contains(first) || line.contains(second);
    }
}
