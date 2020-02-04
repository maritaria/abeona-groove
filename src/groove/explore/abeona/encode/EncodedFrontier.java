package groove.explore.abeona.encode;

import abeona.frontiers.Frontier;
import abeona.frontiers.QueueFrontier;
import abeona.frontiers.TreeMapFrontier;
import groove.explore.encode.EncodedType;
import groove.explore.encode.EncodedTypeEditor;
import groove.explore.prettyparse.PIdentifier;
import groove.explore.prettyparse.POptionalExtension;
import groove.explore.prettyparse.PSequence;
import groove.explore.prettyparse.SerializedParser;
import groove.explore.strategy.MinimaxStrategy;
import groove.grammar.Grammar;
import groove.grammar.host.ValueNode;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;
import groove.lts.RuleTransition;
import groove.util.parse.FormatException;

import java.util.Comparator;
import java.util.Objects;

import static groove.explore.abeona.AbeonaHelpers.findRuleTransition;
import static groove.explore.abeona.encode.EncodingHelpers.createRuleParameterBindingParser;

public class EncodedFrontier implements EncodedType<Frontier<GraphState>, String> {
    private static final String EXTENSION_SEPARATOR = ":";

    public static SerializedParser createParser(String argName) {
        return new PSequence(new PIdentifier(argName),
                new POptionalExtension(EXTENSION_SEPARATOR, argName, createRuleParameterBindingParser(argName)));
    }

    @Override
    public EncodedTypeEditor<Frontier<GraphState>, String> createEditor(GrammarModel grammar) {
        return null;
    }

    @Override
    public Frontier<GraphState> parse(Grammar rules, String source) throws FormatException {
        final var parts = source.split(EXTENSION_SEPARATOR);

        final var type = parts[0];
        final var extension = parts.length > 1 ? parts[1] : null;

        switch (type) {
            case "fifo":
                return QueueFrontier.fifoFrontier();
            case "lifo":
                return QueueFrontier.lifoFrontier();
            case "ordered": {
                if (extension == null) {
                    throw new FormatException("Option 'ordered' requires a rule to be specified (ordered:<rule_name>)");
                }
                try {
                    final var binding = EncodingHelpers.parseRuleParameterBinding(rules, extension);
                    return TreeMapFrontier.<GraphState>withCollisions(createStateComparator(binding),
                            Objects::hashCode);
                } catch (FormatException ex) {
                    throw new FormatException("Invalid rule for ordered frontier: " + ex.getMessage());
                }
            }
            default:
                throw new FormatException("unknown frontier type: '" + type + "'");
        }
    }

    private static Comparator<GraphState> createStateComparator(RuleParameterBinding binding) {
        return Comparator.comparing(state -> measureStateWithRuleTransition(state, binding));
    }

    private static int measureStateWithRuleTransition(GraphState state, RuleParameterBinding binding) {
        return findRuleTransition(binding.getRule(), state).map(ruleTransition -> measureRuleTransition(ruleTransition,
                binding.getParameter())).orElse(Integer.MAX_VALUE);
    }

    private static int measureRuleTransition(RuleTransition transition, int paramNum) {
        final var value = MinimaxStrategy.getParameter(transition, paramNum);
        return (Integer) ((ValueNode) value).toJavaValue();
    }
}
