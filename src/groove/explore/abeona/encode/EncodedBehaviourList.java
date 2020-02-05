package groove.explore.abeona.encode;

import abeona.behaviours.ExplorationBehaviour;
import groove.explore.abeona.AbeonaStrategyTemplate;
import groove.explore.abeona.behaviours.*;
import groove.explore.encode.EncodedType;
import groove.explore.encode.EncodedTypeEditor;
import groove.explore.prettyparse.PAll;
import groove.explore.prettyparse.SerializedParser;
import groove.grammar.Grammar;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;
import groove.util.parse.FormatException;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EncodedBehaviourList implements EncodedType<List<ExplorationBehaviour<GraphState>>, String> {
    public static final String BEHAVIOUR_SEPARATOR = AbeonaStrategyTemplate.ARGUMENT_SEPARATOR;
    public static final String OPTION_SEPARATOR = ":";

    public static SerializedParser createParser(String argName) {
        return new PAll(argName);
    }

    private static final EncodedBehaviour[] encoders = new EncodedBehaviour[]{
            new EncodedTerminationBehaviour(),
            new EncodedFrontierFilterBehaviour(),
            new EncodedFrontierCapacityBehaviour(),
            new EncodedDepthLimitBehaviour()
    };

    @Override
    public List<ExplorationBehaviour<GraphState>> parse(Grammar rules, String source) throws FormatException {
        final var result = new LinkedList<ExplorationBehaviour<GraphState>>();

        String[] parts = source.split(BEHAVIOUR_SEPARATOR);

        for (String part : parts) {
            for (var encodedBehaviour : encoders) {
                final var keyword = encodedBehaviour.getEncodingKeyword();
                if (part.startsWith(keyword)) {
                    final var args = part.substring(keyword.length() + OPTION_SEPARATOR.length());
                    final var behaviour = encodedBehaviour.parse(rules, args);
                    result.push(behaviour);
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public EncodedTypeEditor<List<ExplorationBehaviour<GraphState>>, String> createEditor(GrammarModel grammar) {
        return new Editor(grammar);
    }

    private static class Editor extends EncodedTypeEditor<List<ExplorationBehaviour<GraphState>>, String> {
        private final JPanel editors;
        private final Map<EncodedBehaviour, EncodedTypeEditor<ExplorationBehaviour<GraphState>, String>> controlMap = new HashMap<>();

        public Editor(GrammarModel grammar) {
            super(grammar, new BorderLayout(0, 0));

            editors = new JPanel();
            editors.setLayout(new BoxLayout(editors, BoxLayout.Y_AXIS));

            for (var encoder : encoders) {
                final var editor = new EncodedBehaviourEditorPresenter(grammar, encoder);
                editor.addTemplateListener(this::notifyTemplateListeners);
                controlMap.put(encoder, editor);
                editors.add(editor);
            }

            refresh();
            add(editors, BorderLayout.CENTER);
        }

        @Override
        public String getCurrentValue() {
            final var result = Arrays.stream(encoders).map(encoder -> {
                final var editor = controlMap.get(encoder);
                String value = editor.getCurrentValue();
                return value.isEmpty() ? "" : encoder.getEncodingKeyword() + OPTION_SEPARATOR + value;
            }).filter(Predicate.not(String::isEmpty)).collect(Collectors.joining(BEHAVIOUR_SEPARATOR));
            System.out.println("Serialized behaviours: " + result);
            return result;
        }

        @Override
        public void setCurrentValue(String source) {
            final String[] parts = source.split(BEHAVIOUR_SEPARATOR);
            for (String part : parts) {
                for (var encoder : encoders) {
                    final var keyword = encoder.getEncodingKeyword();
                    if (part.startsWith(keyword)) {
                        final var editor = controlMap.get(encoder);
                        final var args = part.substring(keyword.length() + OPTION_SEPARATOR.length());
                        editor.setCurrentValue(args);
                        break;
                    }
                }
            }
        }

        @Override
        public void refresh() {
            for (var encoder : encoders) {
                final var editor = controlMap.get(encoder);
                editor.refresh();
            }
        }
    }
}
