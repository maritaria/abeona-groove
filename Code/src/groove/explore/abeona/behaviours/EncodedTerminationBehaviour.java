package groove.explore.abeona.behaviours;

import abeona.Query;
import abeona.TransitionEvaluationEvent;
import abeona.behaviours.AbstractBehaviour;
import abeona.behaviours.ExplorationBehaviour;
import groove.explore.abeona.AbeonaHelpers;
import groove.explore.abeona.encode.EncodingHelpers;
import groove.explore.encode.EncodedTypeEditor;
import groove.grammar.Grammar;
import groove.grammar.QualName;
import groove.grammar.model.GrammarModel;
import groove.grammar.model.ResourceKind;
import groove.grammar.model.RuleModel;
import groove.gui.util.WrapLayout;
import groove.lts.GraphState;
import groove.util.parse.FormatException;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import static groove.explore.abeona.encode.EncodedBehaviourList.OPTION_SEPARATOR;
import static groove.explore.abeona.encode.EncodingHelpers.listRulesWithoutInputs;

public class EncodedTerminationBehaviour implements EncodedBehaviour {

    @Override
    public String getEncodingKeyword() {
        return "termination";
    }

    @Override
    public String getDisplayLabel() {
        return "Terminate on rule";
    }

    @Override
    public EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> createEditor(GrammarModel grammar) {
        return new Editor(grammar);
    }

    @Override
    public ExplorationBehaviour<GraphState> parse(Grammar rules, String source) throws FormatException {
        final var parts = source.split(OPTION_SEPARATOR);

        if (parts.length < 1 || parts.length > 2) {
            throw new FormatException("termination behaviour needs 1 or 2 args, found " + parts.length + " instead");
        }
        final var ruleName = parts[0];
        final var amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        if (amount <= 0) {
            throw new FormatException("termination with a threshold amount must have a positive amount (>0), but got " + amount);
        }
        final var rule = EncodingHelpers.parseRule(rules, ruleName);
        return new ThresholdedTerminationBehaviour(state -> AbeonaHelpers.findRuleTransition(rule, state).isPresent(),
                amount);
    }

    private static class ThresholdedTerminationBehaviour extends AbstractBehaviour<GraphState> {
        private final Predicate<GraphState> goalPredicate;
        private final int threshold;
        private WeakHashMap<Query<GraphState>, Integer> counters = new WeakHashMap<>();

        public ThresholdedTerminationBehaviour(Predicate<GraphState> goalPredicate, int threshold) {
            this.goalPredicate = goalPredicate;
            this.threshold = threshold;
        }

        @Override
        public void attach(Query<GraphState> query) {
            tapQueryBehaviour(query, query.onStateDiscovery, this::onDiscovery);
        }

        private void onDiscovery(TransitionEvaluationEvent<GraphState> event) {
            if (goalPredicate.test(event.getTransition().getTargetState())) {
                final var query = event.getQuery();
                final var updated = counters.compute(query, (unused, current) -> current == null ? 1 : current + 1);
                if (updated >= threshold) {
                    event.abortExploration();
                }
            }
        }
    }

    private static class Editor extends EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> {
        private final JComboBox<RuleModel> rules = new JComboBox<>();
        private final JSpinner amount = new JSpinner();

        public Editor(GrammarModel grammar) {
            super(grammar, new WrapLayout(FlowLayout.LEFT, 0, 0));

            amount.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
            refresh();

            add(rules);
            add(amount);
        }

        @Override
        public void refresh() {
            final var grammar = getGrammar();
            final var rule = getRule();
            rules.removeAllItems();
            listRulesWithoutInputs(grammar).forEach(rules::addItem);
            setRule(rule);
        }

        private RuleModel getRule() {
            return rules.getItemAt(rules.getSelectedIndex());
        }

        private void setRule(RuleModel rule) {
            rules.setSelectedItem(rule);
        }

        private int getAmount() {
            return (int) amount.getValue();
        }

        private void setAmount(int value) {
            amount.setValue(value);
        }

        @Override
        public String getCurrentValue() {
            final var rule = getRule();
            if (rule == null) return "";
            return rule.getQualName() + OPTION_SEPARATOR + getAmount();
        }

        @Override
        public void setCurrentValue(String value) {
            if (value.isEmpty()) {
                setRule(null);
                setAmount(1);
            } else {
                final var parts = value.split(OPTION_SEPARATOR);
                final var qname = QualName.parse(parts[0]);
                setRule(getGrammar().getRuleModel(qname));
                setAmount(Integer.parseInt(parts[1]));
            }
        }
    }
}
