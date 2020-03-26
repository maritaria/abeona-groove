package groove.explore.abeona;

import groove.grammar.Rule;
import groove.lts.GraphState;
import groove.lts.RuleTransition;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public final class AbeonaHelpers {

    public static Stream<RuleTransition> listOutgoingRuleTransitions(GraphState state) {
        return Stream.of(listCachedRuleTransitions(state), listMatcherRuleTransitions(state))
                .flatMap(Function.identity());
    }

    public static Stream<RuleTransition> listCachedRuleTransitions(GraphState state) {
        return state.getRuleTransitions().stream();
    }

    public static Stream<RuleTransition> listMatcherRuleTransitions(GraphState state) {
        return state.getMatches().stream().map(matchResult -> {
            try {
                return state.applyMatch(matchResult);
            } catch (InterruptedException e) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public static Optional<RuleTransition> findRuleTransition(Rule rule, GraphState state) {
        return Stream.of(findCachedRuleTransition(rule, state), findMatchedRuleTransition(rule, state))
                .flatMap(Function.identity())
                .findFirst();
    }

    private static Stream<RuleTransition> findCachedRuleTransition(Rule targetRule, GraphState state) {
        return state.getRuleTransitions()
                .stream()
                .filter(ruleTransition -> ruleTransition.getEvent().getRule().equals(targetRule));
    }

    private static Stream<RuleTransition> findMatchedRuleTransition(Rule targetRule, GraphState state) {
        return state.getMatches()
                .stream()
                .filter(matchResult -> matchResult.getAction().equals(targetRule))
                .map(matchResult -> {
                    try {
                        return state.applyMatch(matchResult);
                    } catch (InterruptedException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }

}
