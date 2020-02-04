package groove.explore.abeona.encode;

import abeona.behaviours.ExplorationBehaviour;
import groove.explore.encode.EncodedType;
import groove.explore.encode.EncodedTypeEditor;
import groove.explore.abeona.AbeonaStrategyTemplate;
import groove.explore.abeona.behaviours.EncodedBehaviour;
import groove.explore.abeona.behaviours.EncodedTerminationBehaviour;
import groove.explore.prettyparse.*;
import groove.grammar.Grammar;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;
import groove.util.parse.FormatException;

import java.util.LinkedList;
import java.util.List;

public class EncodedBehaviourList implements EncodedType<List<ExplorationBehaviour<GraphState>>, String> {
    public static final String BEHAVIOUR_SEPARATOR = AbeonaStrategyTemplate.ARGUMENT_SEPARATOR;

    private static final EncodedBehaviour[] encoders = new EncodedBehaviour[]{new EncodedTerminationBehaviour()};

    public static SerializedParser createParser(String argName) {
        return new PAll(argName);
    }

    @Override
    public EncodedTypeEditor<List<ExplorationBehaviour<GraphState>>, String> createEditor(GrammarModel grammar) {
        return null;
    }

    @Override
    public List<ExplorationBehaviour<GraphState>> parse(Grammar rules, String source) throws FormatException {
        final var result = new LinkedList<ExplorationBehaviour<GraphState>>();

        String[] parts = source.split(BEHAVIOUR_SEPARATOR);

        for(String part : parts) {
            for (var encodedBehaviour : encoders) {
                if (part.startsWith(encodedBehaviour.getEncodingKeyword())) {
                    final var behaviour = encodedBehaviour.parse(rules, part);
                    result.push(behaviour);
                    break;
                }
            }
        }

        return result;
    }
}
