package groove.explore.abeona.encode;

import groove.grammar.Rule;

import java.util.Objects;

public class RuleParameterBinding {
    private final Rule rule;
    private final int parameter;

    public RuleParameterBinding(Rule rule, int parameter) {
        if (rule == null) {
            throw new IllegalArgumentException("rule is null");
        }
        this.rule = rule;
        this.parameter = parameter;
    }

    public Rule getRule() {
        return rule;
    }

    public int getParameter() {
        return parameter;
    }

    @Override
    public String toString() {
        return rule.getQualName() + "#" + parameter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RuleParameterBinding that = (RuleParameterBinding) o;
        return parameter == that.parameter && rule.equals(that.rule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule, parameter);
    }
}
