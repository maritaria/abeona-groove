package groove.explore.abeona;

import abeona.behaviours.ExplorationBehaviour;
import abeona.frontiers.Frontier;
import abeona.heaps.Heap;
import groove.explore.StrategyValue;
import groove.explore.encode.Template;
import groove.explore.abeona.encode.EncodedBehaviourList;
import groove.explore.abeona.encode.EncodedFrontier;
import groove.explore.abeona.encode.EncodedHeap;
import groove.explore.prettyparse.PLiteral;
import groove.explore.prettyparse.PSequence;
import groove.explore.prettyparse.SerializedParser;
import groove.explore.strategy.AbeonaStrategy;
import groove.explore.strategy.Strategy;
import groove.lts.GraphState;

import java.util.List;

public class AbeonaStrategyTemplate extends Template.TemplateN<Strategy> {
    public static final String ARGUMENT_SEPARATOR = ",";

    public AbeonaStrategyTemplate(StrategyValue strategyValue) {
        super(
                strategyValue,
                createParser(),
                new String[]{"frontier", "heap", "behaviours"},
                new EncodedFrontier(),
                new EncodedHeap(),
                new EncodedBehaviourList());

    }

    private static SerializedParser createParser() {
        return new PSequence(
                EncodedFrontier.createParser("frontier"),
                new PLiteral(ARGUMENT_SEPARATOR),
                EncodedHeap.createParser("heap"),
                new PLiteral(ARGUMENT_SEPARATOR),
                EncodedBehaviourList.createParser("behaviours"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Strategy create(Object[] arguments) {
        final var frontier = (Frontier<GraphState>) arguments[0];
        final var heap = (Heap<GraphState>) arguments[1];
        final var behaviours = (List<ExplorationBehaviour<GraphState>>) arguments[2];
        return new AbeonaStrategy(frontier, heap, behaviours);
    }
}
