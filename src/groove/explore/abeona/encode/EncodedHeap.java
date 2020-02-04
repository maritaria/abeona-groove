package groove.explore.abeona.encode;

import abeona.heaps.Heap;
import abeona.heaps.NullHeap;
import groove.explore.encode.EncodedType;
import groove.explore.encode.EncodedTypeEditor;
import groove.explore.prettyparse.PIdentifier;
import groove.explore.prettyparse.SerializedParser;
import groove.grammar.Grammar;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;
import groove.util.parse.FormatException;

import static groove.lts.Status.Flag.KNOWN;

public class EncodedHeap implements EncodedType<Heap<GraphState>, String> {
    public static SerializedParser createParser(String argName) {
        return new PIdentifier(argName);
    }

    @Override
    public EncodedTypeEditor<Heap<GraphState>, String> createEditor(GrammarModel grammar) {
        return null;
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
