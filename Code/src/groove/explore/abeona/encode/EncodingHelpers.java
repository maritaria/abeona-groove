package groove.explore.abeona.encode;

import groove.explore.prettyparse.*;
import groove.grammar.Grammar;
import groove.grammar.QualName;
import groove.grammar.Rule;
import groove.grammar.UnitPar;
import groove.grammar.model.GrammarModel;
import groove.grammar.model.ResourceKind;
import groove.grammar.model.RuleModel;
import groove.util.parse.FormatException;

import java.util.stream.Stream;

public class EncodingHelpers {
    private static final String RULE_PARAMETER_SEPARATOR = "#";

    /**
     * Attempts to finds the Rule with the given name. If such a rule does
     * not exist, or is not enabled, a FormatException is thrown.
     */
    public static RuleParameterBinding parseRuleParameterBinding(Grammar rules, String input) throws FormatException {
        final var pars = input.split(RULE_PARAMETER_SEPARATOR);
        if (pars.length != 2) {
            throw new FormatException("Invalid format: must be [qualname]" + RULE_PARAMETER_SEPARATOR + "[parnum]");
        }
        final var name = pars[0];
        QualName qualName = QualName.parse(name);
        Rule rule = rules.getRule(qualName);
        if (rule == null) {
            throw new FormatException("'" + qualName + "' is not an enabled rule in the loaded grammar.");
        }
        try {
            final var paramIndex = Integer.parseInt(pars[1]);
            final var signature = rule.getSignature().getPars();
            if (paramIndex >= signature.size()) {
                throw new FormatException("'" + qualName + "' only has " + signature
                        .size() + " parameters, the index " + paramIndex + " is out of range");
            }
            final var param = signature.get(paramIndex);
            if (param == null) {
                throw new FormatException("'" + qualName + "' does not have a parameter at index " + paramIndex);
            }
            if (!param.isOutOnly()) {
                throw new FormatException("'" + qualName + "' does not have an out-only parameter at index " + paramIndex);
            }
            return new RuleParameterBinding(rule, paramIndex);
        } catch (NumberFormatException ex) {
            throw new FormatException("The given parameter index is not a number (given: '" + pars[1] + "')");
        }
    }

    public static SerializedParser createRuleParameterBindingParser(String argName) {
        return new PSequence(new PIdentifier(argName),
                new PLiteral(RULE_PARAMETER_SEPARATOR, argName),
                new PNumber(argName));
    }

    /**
     * Attempts to finds the Rule with the given name. If such a rule does
     * not exist, or is not enabled, a FormatException is thrown.
     */
    public static Rule parseRule(Grammar rules, String input) throws FormatException {
        QualName qualName = QualName.parse(input);
        Rule rule = rules.getRule(qualName);
        if (rule == null) {
            throw new FormatException("'" + qualName + "' is not an enabled rule in the loaded grammar.");
        }
        return rule;
    }

    public static SerializedParser createRuleParser(String argName) {
        return new PIdentifier(argName);
    }

    public static Stream<RuleModel> listRulesWithoutInputs(GrammarModel grammar) {
        return grammar.getActiveNames(ResourceKind.RULE).stream().map(grammar::getRuleModel).filter(rule -> {
            try {
                return rule.getSignature().getPars().stream().noneMatch(UnitPar::isInOnly);
            } catch (FormatException e) {
                return false;
            }
        });
    }

    public static String trimPrefix(String source, String prefix) {
        if (source.startsWith(prefix)) {
            return source.substring(prefix.length());
        } else {
            return source;
        }
    }

    public static String trimSuffix(String source, String suffix) {
        if (source.endsWith(suffix)) {
            return source.substring(0, source.length() - suffix.length());
        } else {
            return source;
        }
    }
}
