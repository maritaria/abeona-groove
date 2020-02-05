package groove.explore.abeona.behaviours;

import abeona.behaviours.ExplorationBehaviour;
import abeona.behaviours.TraceCostBehaviour;
import abeona.behaviours.TraceCostLimitBehaviour;
import groove.explore.encode.EncodedTypeEditor;
import groove.grammar.Grammar;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;
import groove.util.parse.FormatException;

import javax.swing.*;
import java.awt.*;

public class EncodedDepthLimitBehaviour implements EncodedBehaviour {
    @Override
    public String getEncodingKeyword() {
        return "depth-limit";
    }

    @Override
    public String getDisplayLabel() {
        return "Limit max search depth";
    }

    @Override
    public ExplorationBehaviour<GraphState> parse(Grammar rules, String source) throws FormatException {
        final var maxDepth = Integer.parseInt(source);
        if (maxDepth < 0) {
            throw new FormatException("Illegal depth-limit parameter, must be non-negative");
        }
        final var traceCost = new TraceCostBehaviour<GraphState>(t -> 1);
        return new TraceCostLimitBehaviour<>(traceCost, maxDepth);
    }

    @Override
    public EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> createEditor(
            GrammarModel grammar
    ) {
        return new Editor(grammar);
    }

    private static class Editor extends EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> {
        private final JSpinner depth = new JSpinner();

        public Editor(GrammarModel grammar) {
            super(grammar, new FlowLayout(FlowLayout.LEFT, 0, 0));
            depth.setModel(new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
            depth.addChangeListener(unused -> notifyTemplateListeners());
            refresh();
            add(depth);
        }

        @Override
        public void refresh() {

        }

        @Override
        public String getCurrentValue() {
            return depth.getValue().toString();
        }

        @Override
        public void setCurrentValue(String value) {
            depth.setValue(value.isEmpty() ? 10 : Integer.parseInt(value));
        }
    }
}
