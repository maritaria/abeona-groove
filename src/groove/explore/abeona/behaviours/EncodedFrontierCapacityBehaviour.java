package groove.explore.abeona.behaviours;

import abeona.behaviours.ExplorationBehaviour;
import abeona.behaviours.FrontierCapacityBehaviour;
import groove.explore.encode.EncodedTypeEditor;
import groove.grammar.Grammar;
import groove.grammar.model.GrammarModel;
import groove.gui.util.WrapLayout;
import groove.lts.GraphState;
import groove.util.parse.FormatException;

import javax.swing.*;
import java.awt.*;

public class EncodedFrontierCapacityBehaviour implements EncodedBehaviour {
    @Override
    public String getEncodingKeyword() {
        return "frontier-capacity";
    }

    @Override
    public String getDisplayLabel() {
        return "Limit frontier size";
    }

    @Override
    public ExplorationBehaviour<GraphState> parse(Grammar rules, String source) throws FormatException {
        final var capacity = Integer.parseInt(source);
        return new FrontierCapacityBehaviour<>(capacity);
    }

    @Override
    public EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> createEditor(
            GrammarModel grammar
    ) {
        return new Editor(grammar);
    }

    private static class Editor extends EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> {
        private final JSpinner capacity = new JSpinner();

        public Editor(GrammarModel grammar) {
            super(grammar, new WrapLayout(FlowLayout.LEFT, 0, 0));
            capacity.setModel(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 10));
            capacity.addChangeListener(unused -> notifyTemplateListeners());

            refresh();

            add(capacity);
        }

        @Override
        public void refresh() {

        }

        private int getCapacity() {
            return (int) capacity.getValue();
        }

        private void setCapacity(int value) {
            capacity.setValue(value);
        }

        @Override
        public String getCurrentValue() {
            return Integer.toString(getCapacity());
        }

        @Override
        public void setCurrentValue(String value) {
            if (!value.isEmpty()) {
                setCapacity(Integer.parseInt(value));
            }
        }
    }
}
