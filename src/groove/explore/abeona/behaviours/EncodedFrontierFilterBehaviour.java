package groove.explore.abeona.behaviours;

import abeona.behaviours.ExplorationBehaviour;
import abeona.behaviours.FrontierFilterBehaviour;
import groove.explore.abeona.AbeonaHelpers;
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

import static groove.explore.abeona.encode.EncodingHelpers.listRulesWithoutInputs;

public class EncodedFrontierFilterBehaviour implements EncodedBehaviour {
    @Override
    public String getEncodingKeyword() {
        return "filter";
    }

    @Override
    public String getDisplayLabel() {
        return "Require rule on frontier states";
    }

    @Override
    public ExplorationBehaviour<GraphState> parse(Grammar rules, String source) throws FormatException {
        final var qname = QualName.parse(source);
        final var rule = rules.getRule(qname);
        return new FrontierFilterBehaviour<>(state -> AbeonaHelpers.findRuleTransition(rule, state).isPresent());
    }

    @Override
    public EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> createEditor(
            GrammarModel grammar
    ) {
        return new Editor(grammar);
    }

    private static class Editor extends EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> {
        private final JComboBox<RuleModel> rules = new JComboBox<>();

        public Editor(GrammarModel grammar) {
            super(grammar, new WrapLayout(FlowLayout.LEFT, 0, 0));

            rules.addActionListener(unused -> notifyTemplateListeners());

            refresh();
            add(rules);
        }

        private RuleModel getRule() {
            return rules.getItemAt(rules.getSelectedIndex());
        }

        private void setRule(RuleModel rule) {
            rules.setSelectedItem(rule);
        }

        @Override
        public void refresh() {
            final var selectedRule = getRule();
            rules.removeAllItems();
            final var grammar = getGrammar();
            listRulesWithoutInputs(grammar).forEach(rules::addItem);
            setRule(selectedRule);
        }

        @Override
        public String getCurrentValue() {
            final var rule = getRule();
            return rule != null ? rule.getQualName().toString() : "";
        }

        @Override
        public void setCurrentValue(String value) {
            if (value.isEmpty()) {
                setRule(null);
            } else {
                final var qualname = QualName.parse(value);
                final var rule = getGrammar().getRuleModel(qualname);
                setRule(rule);
            }
        }

    }
}
