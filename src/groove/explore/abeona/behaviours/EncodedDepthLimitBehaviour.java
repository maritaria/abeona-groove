package groove.explore.abeona.behaviours;

import abeona.Transition;
import abeona.behaviours.ExplorationBehaviour;
import abeona.behaviours.TraceCostBehaviour;
import abeona.behaviours.TraceCostLimitBehaviour;
import groove.explore.encode.EncodedTypeEditor;
import groove.grammar.Grammar;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;
import groove.lts.RuleTransition;
import groove.util.parse.FormatException;

import javax.swing.*;
import java.awt.*;
import java.util.function.ToDoubleFunction;

import static groove.explore.abeona.encode.EncodingHelpers.trimSuffix;

public class EncodedDepthLimitBehaviour implements EncodedBehaviour {
    private static final String ONLY_REAL_STEPS_SUFFIX = ":only-real-steps";

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
        String remainder = source;

        boolean onlyRealSteps = false;
        if (remainder.endsWith(ONLY_REAL_STEPS_SUFFIX)) {
            onlyRealSteps = true;
            remainder = trimSuffix(remainder, ONLY_REAL_STEPS_SUFFIX);
        }

        final var maxDepth = Integer.parseInt(remainder);
        if (maxDepth < 0) {
            throw new FormatException("Illegal depth-limit parameter, must be non-negative");
        }
        ToDoubleFunction<Transition<GraphState>> costFunction;
        if (onlyRealSteps) {
            costFunction = t -> ((RuleTransition) t.getUserdata()).isRealStep() ? 1 : 0;
        } else {
            costFunction = t -> 1;
        }
        final var traceCost = new TraceCostBehaviour<GraphState>(costFunction);
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
        private final JCheckBox onlyCountRealSteps = new JCheckBox("Only count real steps");

        public Editor(GrammarModel grammar) {
            super(grammar, new FlowLayout(FlowLayout.LEFT, 0, 0));
            depth.setModel(new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
            depth.addChangeListener(unused -> notifyTemplateListeners());
            refresh();
            add(depth);
            add(onlyCountRealSteps);
        }

        @Override
        public void refresh() {

        }

        @Override
        public String getCurrentValue() {
            String result = depth.getValue().toString();
            if (onlyCountRealSteps.isSelected()) {
                result += ONLY_REAL_STEPS_SUFFIX;
            }
            return result;
        }

        @Override
        public void setCurrentValue(String value) {
            String remainder = value;
            if (remainder.endsWith(ONLY_REAL_STEPS_SUFFIX)) {
                onlyCountRealSteps.setSelected(true);
                remainder = trimSuffix(remainder, ONLY_REAL_STEPS_SUFFIX);
            } else {
                onlyCountRealSteps.setSelected(false);
            }
            depth.setValue(remainder.isEmpty() ? 10 : Integer.parseInt(value));
        }
    }
}
