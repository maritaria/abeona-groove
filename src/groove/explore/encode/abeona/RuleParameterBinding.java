package groove.explore.encode.abeona;

import groove.grammar.Rule;

public class RuleParameterBinding {
    private final Rule rule;
    private final int parameter;

    public RuleParameterBinding(Rule rule, int parameter) {
        this.rule = rule;
        this.parameter = parameter;
    }

    public Rule getRule() {
        return rule;
    }

    public int getParameter() {
        return parameter;
    }
}
