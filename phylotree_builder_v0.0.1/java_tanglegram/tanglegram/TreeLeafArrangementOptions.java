package tanglegram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TreeLeafArrangementOptions {
    private static final List<TreeLeafArrangementRule> DEFAULT_RULE_ORDER = List.of(
            TreeLeafArrangementRule.CLADE_SIZE,
            TreeLeafArrangementRule.LEAF_NAME_STRING,
            TreeLeafArrangementRule.BRANCH_LENGTH);

    private final boolean enabled;
    private final List<TreeLeafArrangementRule> ruleOrder;
    private final TreeLeafArrangementDirection direction;

    TreeLeafArrangementOptions(
            boolean enabled,
            List<TreeLeafArrangementRule> ruleOrder,
            TreeLeafArrangementDirection direction) {
        this.enabled = enabled;
        this.ruleOrder = sanitizeRuleOrder(ruleOrder);
        this.direction = direction == null ? TreeLeafArrangementDirection.UP : direction;
    }

    static TreeLeafArrangementOptions defaults() {
        return new TreeLeafArrangementOptions(true, DEFAULT_RULE_ORDER, TreeLeafArrangementDirection.UP);
    }

    static TreeLeafArrangementOptions disabled() {
        return new TreeLeafArrangementOptions(false, DEFAULT_RULE_ORDER, TreeLeafArrangementDirection.UP);
    }

    boolean enabled() {
        return enabled;
    }

    List<TreeLeafArrangementRule> ruleOrder() {
        return ruleOrder;
    }

    TreeLeafArrangementDirection direction() {
        return direction;
    }

    TreeLeafArrangementOptions withEnabled(boolean enabled) {
        return new TreeLeafArrangementOptions(enabled, ruleOrder, direction);
    }

    private static List<TreeLeafArrangementRule> sanitizeRuleOrder(List<TreeLeafArrangementRule> rules) {
        List<TreeLeafArrangementRule> sanitized = new ArrayList<>();
        if (rules != null) {
            for (TreeLeafArrangementRule rule : rules) {
                if (rule != null && !sanitized.contains(rule)) {
                    sanitized.add(rule);
                }
            }
        }
        for (TreeLeafArrangementRule rule : DEFAULT_RULE_ORDER) {
            if (!sanitized.contains(rule)) {
                sanitized.add(rule);
            }
        }
        return Collections.unmodifiableList(sanitized);
    }
}
