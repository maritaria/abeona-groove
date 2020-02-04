package groove.explore.abeona.encode;

import abeona.heaps.Heap;
import abeona.heaps.NullHeap;
import groove.explore.encode.EncodedEnumeratedType;
import groove.explore.prettyparse.PIdentifier;
import groove.explore.prettyparse.SerializedParser;
import groove.grammar.Grammar;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;
import groove.util.parse.FormatException;

import java.util.HashMap;
import java.util.Map;

import static groove.lts.Status.Flag.KNOWN;

public class EncodedHeap extends EncodedEnumeratedType<Heap<GraphState>> {
    public static SerializedParser createParser(String argName) {
        return new PIdentifier(argName);
    }

    @Override
    public Map<String, String> generateOptions(GrammarModel grammar) {
        final var result = new HashMap<String, String>();
        result.put("flags", "Default (flags)");
        result.put("null", "No heap (null)");
        return result;
    }

    @Override
    public Heap<GraphState> parse(Grammar rules, String source) throws FormatException {
        switch (source) {
            case "flags":
                return new GtsHeap();
            case "null":
                return new NullHeap<>();
            default:
                throw new FormatException("Unknown heap type: '" + source + "'");
        }
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
