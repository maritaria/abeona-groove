package groove.explore.strategy;

import abeona.ExplorationEvent;
import abeona.Query;
import abeona.StateEvaluationEvent;
import abeona.Transition;
import abeona.frontiers.QueueFrontier;
import abeona.frontiers.TreeMapFrontier;
import abeona.heaps.Heap;
import groove.explore.encode.abeona.RuleParameterBinding;
import groove.explore.result.Acceptor;
import groove.grammar.Rule;
import groove.grammar.host.ValueNode;
import groove.lts.GTS;
import groove.lts.GraphState;
import groove.lts.RuleTransition;
import groove.match.MatcherFactory;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static groove.lts.Status.Flag.KNOWN;

public final class AbeonaStrategy extends Strategy {
    private final Query<GraphState> query;
    private boolean terminated = false;
    private GraphState lastExplored = null;
    private GTS gts;
    private Acceptor acceptor;

    public AbeonaStrategy() {
        final var frontier = QueueFrontier.<GraphState>lifoFrontier();
        final var heap = new GtsHeap();
        this.query = new Query<GraphState>(frontier, heap, this::nextFunction);
        configureQuery();
    }

    public AbeonaStrategy(RuleParameterBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("binding is null");
        }
        final var frontier = TreeMapFrontier.<GraphState>withCollisions(this.createStateComparator(binding),
                Objects::hashCode);
        final var heap = new GtsHeap();
        this.query = new Query<GraphState>(frontier, heap, this::nextFunction);
        configureQuery();
    }

    private void configureQuery() {
        query.beforeStateEvaluation.tap(this::beforeStateEvaluation);
        query.pickNextState.tap(this::onPickNextState);
        query.afterStateEvaluation.tap(this::beforeStatePicked);
    }

    private void beforeStatePicked(ExplorationEvent<GraphState> event) {
        if (acceptor.done()) {
            event.abortExploration();
        }
    }

    private Comparator<GraphState> createStateComparator(RuleParameterBinding binding) {
        return Comparator.comparing(state -> measureStateWithRuleTransition(state, binding));
    }

    private int measureStateWithRuleTransition(GraphState state, RuleParameterBinding binding) {
        return findRuleTransition(binding.getRule(), state).map(ruleTransition -> this.measureRuleTransition(
                ruleTransition,
                binding.getParameter())).orElse(Integer.MAX_VALUE);
    }

    private Optional<RuleTransition> findRuleTransition(Rule rule, GraphState state) {
        return Stream.of(findCachedRuleTransition(rule, state), findMatchedRuleTransition(rule, state))
                .flatMap(Function.identity())
                .findFirst();
    }

    private Stream<RuleTransition> findCachedRuleTransition(Rule targetRule, GraphState state) {
        return state.getRuleTransitions()
                .stream()
                .filter(ruleTransition -> ruleTransition.getEvent().getRule().equals(targetRule));
    }

    private Stream<RuleTransition> findMatchedRuleTransition(Rule targetRule, GraphState state) {
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

    private int measureRuleTransition(RuleTransition transition, int paramNum) {
        final var value = MinimaxStrategy.getParameter(transition, paramNum);
        return (Integer) ((ValueNode) value).toJavaValue();
    }

    @Override
    protected void prepare(GTS gts, GraphState state, Acceptor acceptor) {
        state = state == null ? gts.startState() : state; /* GTSStrategy::prepare */
        super.prepare(gts, state, acceptor);
        // TODO: Init query here
        this.query.getFrontier().add(Stream.of(state));
        this.terminated = false;
        this.lastExplored = state;
        gts.addLTSListener(acceptor);
        // GTSStrategy::prepare
        acceptor.addUpdate(gts, state);
        MatcherFactory.instance(gts.isSimple()).setDefaultEngine();
        this.gts = gts;
        this.acceptor = acceptor;
    }

    @Override
    public void finish() {
        gts.removeLTSListener(acceptor);
    }

    @Override
    public boolean hasNext() {
        return !this.terminated && this.query.getFrontier().hasNext();
    }

    @Override
    public GraphState doNext() {
        final var result = query.exploreNext();
        terminated = result.isPresent();
        return lastExplored;
    }

    private Stream<Transition<GraphState>> nextFunction(GraphState state) {
        return state.getMatches().stream().map(match -> {
            try {
                final var ruleTransition = state.applyMatch(match);
                return new Transition<>(state, ruleTransition.target(), ruleTransition);
            } catch (InterruptedException e) {
                throw new RuntimeException("Exploration interrupted while applying matches", e);
            }
        });
    }

    private void beforeStateEvaluation(StateEvaluationEvent<GraphState> event) {
        this.lastExplored = event.getSourceState();
    }

    private GraphState onPickNextState(Query<GraphState> query, Function<Query<GraphState>, GraphState> next) {
        // Based on ClosingStrategy::doNext
        // If a state gets evaluated while a control frame is present some of the outgoing transitions may be filtered out, as such we have to re-queue the state
        final var state = next.apply(query);
        System.out.println("[abeona] Next state: " + state);
        if (state.getActualFrame().isTrial()) {
            query.getFrontier().add(Stream.of(state));
        }
        return state;
    }

    private class GtsHeap implements Heap<GraphState> {
        @Override
        public boolean add(GraphState graphState) {
            return graphState.setFlag(KNOWN, true);
        }

        @Override
        public boolean contains(GraphState graphState) {
            return graphState.hasFlag(KNOWN);
        }

        @Override
        public void clear() {
            final var gts = AbeonaStrategy.this.gts;
            gts.removeNodeSet(gts.nodeSet());
        }
    }
}
