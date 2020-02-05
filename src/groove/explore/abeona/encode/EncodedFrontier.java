package groove.explore.abeona.encode;

import abeona.frontiers.Frontier;
import abeona.frontiers.QueueFrontier;
import abeona.frontiers.TreeMapFrontier;
import groove.explore.encode.EncodedType;
import groove.explore.encode.EncodedTypeEditor;
import groove.explore.prettyparse.PIdentifier;
import groove.explore.prettyparse.POptionalExtension;
import groove.explore.prettyparse.PSequence;
import groove.explore.prettyparse.SerializedParser;
import groove.explore.strategy.MinimaxStrategy;
import groove.grammar.Grammar;
import groove.grammar.host.ValueNode;
import groove.grammar.model.GrammarModel;
import groove.grammar.model.ResourceKind;
import groove.gui.dialog.ExplorationDialog;
import groove.gui.util.WrapLayout;
import groove.lts.GraphState;
import groove.lts.RuleTransition;
import groove.util.parse.FormatException;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Objects;

import static groove.explore.abeona.AbeonaHelpers.findRuleTransition;
import static groove.explore.abeona.encode.EncodingHelpers.createRuleParameterBindingParser;

public class EncodedFrontier implements EncodedType<Frontier<GraphState>, String> {
    private static final String EXTENSION_SEPARATOR = ":";

    private enum FrontierType {
        FIFO, LIFO, ORDERED,
    }


    public static SerializedParser createParser(String argName) {
        return new PSequence(new PIdentifier(argName),
                new POptionalExtension(EXTENSION_SEPARATOR, argName, createRuleParameterBindingParser(argName)));
    }

    @Override
    public Frontier<GraphState> parse(Grammar grammar, String source) throws FormatException {
        FrontierType type = parseFrontierType(source);
        switch (type) {
            case FIFO:
                return QueueFrontier.fifoFrontier();
            case LIFO:
                return QueueFrontier.lifoFrontier();
            case ORDERED: {
                try {
                    final var binding = parseRuleBinding(grammar, source);
                    if (binding == null) {
                        throw new FormatException(
                                "Option 'ordered' requires a rule to be specified (ordered:<rule_name>#<par_num>)");
                    }
                    return TreeMapFrontier.<GraphState>withCollisions(createStateComparator(binding),
                            Objects::hashCode);
                } catch (FormatException ex) {
                    throw new FormatException("Invalid rule for ordered frontier: " + ex.getMessage());
                }
            }
            default:
                throw new IllegalStateException();
        }
    }

    private static FrontierType parseFrontierType(String source) throws FormatException {
        final var typeName = source.split(EXTENSION_SEPARATOR)[0].toUpperCase();
        try {
            return FrontierType.valueOf(typeName);
        } catch (IllegalArgumentException ex) {
            throw new FormatException("Unknown frontier type: '" + typeName + "'");
        }
    }

    private static RuleParameterBinding parseRuleBinding(
            Grammar grammar, String source
    ) throws FormatException {
        final var parts = source.split(EXTENSION_SEPARATOR);
        return parts.length > 1 ? EncodingHelpers.parseRuleParameterBinding(grammar, parts[1]) : null;
    }

    private static Comparator<GraphState> createStateComparator(RuleParameterBinding binding) {
        return Comparator.comparing(state -> measureStateWithRuleTransition(state, binding));
    }

    private static int measureStateWithRuleTransition(GraphState state, RuleParameterBinding binding) {
        return findRuleTransition(binding.getRule(), state).map(ruleTransition -> measureRuleTransition(ruleTransition,
                binding.getParameter())).orElse(Integer.MAX_VALUE);
    }

    private static int measureRuleTransition(RuleTransition transition, int paramNum) {
        final var value = MinimaxStrategy.getParameter(transition, paramNum);
        return (Integer) ((ValueNode) value).toJavaValue();
    }

    @Override
    public EncodedTypeEditor<Frontier<GraphState>, String> createEditor(GrammarModel grammar) {
        return new FrontierEditor(grammar);
    }

    private static class FrontierEditor extends EncodedTypeEditor<Frontier<GraphState>, String> {
        private final JComboBox<FrontierType> types = new JComboBox<>();
        private final JComboBox<RuleParameterBinding> rules = new JComboBox<>();

        public FrontierEditor(GrammarModel grammar) {
            super(grammar, new WrapLayout(FlowLayout.LEFT, 0, 0));

            setBackground(ExplorationDialog.INFO_BG_COLOR);

            types.addItem(FrontierType.FIFO);
            types.addItem(FrontierType.LIFO);
            types.addItem(FrontierType.ORDERED);
            types.addActionListener(e -> notifyTemplateListeners());
            types.addActionListener(e -> refresh());

            rules.addActionListener(e -> notifyTemplateListeners());

            refresh();
            add(types);
            add(rules);
        }


        public FrontierType getType() {
            return types.getItemAt(types.getSelectedIndex());
        }

        public RuleParameterBinding getRule() {
            return rules.getItemAt(rules.getSelectedIndex());
        }

        @Override
        public void refresh() {
            delayTemplateNotifications();
            if (getType() == FrontierType.ORDERED) {
                final var selectedRule = getRule();
                rules.removeAllItems();
                final var grammar = getGrammar();
                grammar.getActiveNames(ResourceKind.RULE).stream().map(ruleName -> {
                    try {
                        final var ruleModel = grammar.getRuleModel(ruleName);
                        return ruleModel.toResource();
                    } catch (FormatException e) {
                        return null;
                    }
                }).filter(Objects::nonNull).forEach(rule -> {
                    final var signature = rule.getSignature();
                    final var pars = signature.getPars();
                    for (int index = 0; index < pars.size(); index++) {
                        final var param = pars.get(index);
                        if (param.isOutOnly()) {
                            rules.addItem(new RuleParameterBinding(rule, index));
                        }
                    }
                });
                if (selectedRule != null) {
                    rules.setSelectedItem(selectedRule);
                }
                if (rules.getSelectedIndex() == -1) {
                    rules.setSelectedIndex(0);
                }
                rules.setVisible(rules.getItemCount() > 0);
            } else {
                rules.setVisible(false);
            }
            resumeTemplateNotifications();
        }

        @Override
        public String getCurrentValue() {
            final var type = types.getItemAt(types.getSelectedIndex());
            switch (type) {
                case FIFO:
                case LIFO:
                    return type.name();
                case ORDERED:
                    final var rule = rules.getItemAt(rules.getSelectedIndex());
                    return type.name() + EXTENSION_SEPARATOR + rule.toString();
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public void setCurrentValue(String value) {
            try {
                final var type = parseFrontierType(value);
                types.setSelectedItem(type);
                if (type == FrontierType.ORDERED) {
                    final var binding = parseRuleBinding(getGrammar().toGrammar(), value);
                    rules.setSelectedItem(binding);
                }
            } catch (FormatException e) {
                types.setSelectedItem(null);
                rules.setSelectedItem(null);
            }
        }


        private boolean delayTemplateNotification = false;
        private boolean triggerTemplateNotification = false;

        private void delayTemplateNotifications() {
            delayTemplateNotification = true;

        }

        private void resumeTemplateNotifications() {
            delayTemplateNotification = false;
            if (triggerTemplateNotification) {
                notifyTemplateListeners();
            }
        }

        @Override
        protected void notifyTemplateListeners() {
            if (delayTemplateNotification) {
                triggerTemplateNotification = true;
            } else {
                super.notifyTemplateListeners();
            }
        }
    }
}
