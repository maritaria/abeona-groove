package groove.explore.strategy;

import abeona.ExplorationEvent;
import abeona.Query;
import abeona.StateEvaluationEvent;
import abeona.Transition;
import abeona.behaviours.ExplorationBehaviour;
import abeona.frontiers.Frontier;
import abeona.heaps.Heap;
import groove.explore.abeona.AbeonaHelpers;
import groove.explore.result.Acceptor;
import groove.lts.GTS;
import groove.lts.GraphState;
import groove.match.MatcherFactory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class AbeonaStrategy extends Strategy {
    private final Query<GraphState> query;
    private boolean terminated = false;
    private GraphState lastExplored = null;
    private GTS gts;
    private Acceptor acceptor;

    public AbeonaStrategy(
            Frontier<GraphState> frontier, Heap<GraphState> heap, List<ExplorationBehaviour<GraphState>> behaviours
    ) {
        this.query = new Query<GraphState>(frontier, heap, this::nextFunction);
        behaviours.forEach(this.query::addBehaviour);
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
        return AbeonaHelpers.listOutgoingRuleTransitions(state)
                .map(ruleTransition -> new Transition<>(state, ruleTransition.target(), ruleTransition));
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
}
