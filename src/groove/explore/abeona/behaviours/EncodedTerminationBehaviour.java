package groove.explore.abeona.behaviours;

import abeona.Query;
import abeona.TransitionEvaluationEvent;
import abeona.behaviours.AbstractBehaviour;
import abeona.behaviours.ExplorationBehaviour;
import groove.explore.abeona.AbeonaHelpers;
import groove.explore.abeona.encode.EncodingHelpers;
import groove.explore.encode.EncodedTypeEditor;
import groove.grammar.Grammar;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;
import groove.util.parse.FormatException;

import java.util.WeakHashMap;
import java.util.function.Predicate;

public class EncodedTerminationBehaviour implements EncodedBehaviour {

    @Override
    public String getEncodingKeyword() {
        return "termination";
    }

    @Override
    public EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> createEditor(GrammarModel grammar) {
        return null;
    }

    @Override
    public ExplorationBehaviour<GraphState> parse(Grammar rules, String source) throws FormatException {
        System.out.println("EncodedTerminationBehaviour::parse '" + source + "'");
        final var parts = source.split(":");

        if (parts.length < 2 || parts.length > 3) {
            throw new FormatException("termination behaviour needs 2 or 3 parts, found " + parts.length + " instead");
        }
        final var ruleName = parts[1];
        final var amount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
        if (amount <= 0) {
            throw new FormatException("termination with a threshold amount must have a positive amount (>0), but got " + amount);
        }
        final var rule = EncodingHelpers.parseRule(rules, ruleName);
        return new ThresholdedTerminationBehaviour(
                state -> AbeonaHelpers.findRuleTransition(rule, state).isPresent(),
                amount);
    }

    private static class ThresholdedTerminationBehaviour extends AbstractBehaviour<GraphState> {
        private final Predicate<GraphState> goalPredicate;
        private final int threshold;
        private WeakHashMap<Query<GraphState>, Integer> counters = new WeakHashMap<>();

        public ThresholdedTerminationBehaviour(Predicate<GraphState> goalPredicate, int threshold) {
            this.goalPredicate = goalPredicate;
            this.threshold = threshold;
        }

        @Override
        public void attach(Query<GraphState> query) {
            tapQueryBehaviour(query, query.onStateDiscovery, this::onDiscovery);
        }

        private void onDiscovery(TransitionEvaluationEvent<GraphState> event) {
            if (goalPredicate.test(event.getTransition().getTargetState())) {
                final var query = event.getQuery();
                final var updated = counters.compute(query, (unused, current) -> current == null ? 1 : current + 1);
                if (updated >= threshold) {
                    event.abortExploration();
                }
            }
        }
    }
}
