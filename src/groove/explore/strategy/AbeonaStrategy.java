package groove.explore.strategy;

import abeona.Query;
import abeona.StateEvaluationEvent;
import abeona.Transition;
import abeona.frontiers.QueueFrontier;
import abeona.heaps.Heap;
import groove.explore.result.Acceptor;
import groove.lts.GTS;
import groove.lts.GraphState;

import java.util.function.Function;
import java.util.stream.Stream;

import static groove.lts.Status.Flag.KNOWN;

public final class AbeonaStrategy extends GTSStrategy {
    private final abeona.Query<GraphState> query;
    private boolean terminated = false;
    private GraphState lastExplored = null;

    public AbeonaStrategy() {
        final var frontier = QueueFrontier.<GraphState>lifoFrontier();
        final var heap = new GtsHeap();
        this.query = new Query<GraphState>(frontier, heap, this::nextFunction);
        this.query.beforeStateEvaluation.tap(this::beforeStateEvaluation);
        this.query.pickNextState.tap(this::onPickNextState);
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
        if (state.getActualFrame().isTrial()) {
            query.getFrontier().add(Stream.of(state));
        }
        return state;
    }

    @Override
    protected void prepare(GTS gts, GraphState state, Acceptor acceptor) {
        state = state == null ? gts.startState() : state; /* GTSStrategy::prepare */
        super.prepare(gts, state, acceptor);
        // TODO: Init query here
        this.query.getFrontier().add(Stream.of(state));
        this.terminated = false;
        this.lastExplored = state;
    }

    @Override
    protected GraphState computeNextState() {
        return null;
    }

    @Override
    public boolean hasNext() {
        return this.query.getFrontier().hasNext();
    }

    @Override
    public GraphState doNext() {
        final var result = query.exploreNext();
        terminated = result.isPresent();
        return lastExplored;
    }

    private static class GtsHeap implements Heap<GraphState> {
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
        }
    }
}
