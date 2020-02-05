package groove.explore.abeona.behaviours;

import abeona.behaviours.ExplorationBehaviour;
import groove.explore.encode.EncodedTypeEditor;
import groove.grammar.model.GrammarModel;
import groove.lts.GraphState;

import javax.swing.*;
import java.awt.*;

public final class EncodedBehaviourEditorPresenter extends EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> {
    private final JCheckBox enabled;
    private final EncodedTypeEditor<ExplorationBehaviour<GraphState>, String> editor;

    public EncodedBehaviourEditorPresenter(
            GrammarModel grammar, EncodedBehaviour encoder
    ) {
        super(grammar, null);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder(encoder.getDisplayLabel() + " (" + encoder.getEncodingKeyword() + ")"));

        enabled = new JCheckBox("Enabled");
        enabled.setAlignmentX(Component.LEFT_ALIGNMENT);
        enabled.addChangeListener(unused -> notifyTemplateListeners());
        editor = encoder.createEditor(grammar);
        editor.setAlignmentX(Component.LEFT_ALIGNMENT);

        refresh();
        add(enabled);
        add(editor);
    }

    @Override
    public String getCurrentValue() {
        if (!enabled.isSelected()) {
            return "";
        }
        return editor.getCurrentValue();
    }

    @Override
    public void setCurrentValue(String value) {
        if (value == null || value.isEmpty()) {
            editor.setCurrentValue("");
            enabled.setSelected(false);
        } else {
            editor.setCurrentValue(value);
            enabled.setSelected(true);
        }
    }

    @Override
    public void refresh() {
        delayTemplateNotifications();
        editor.refresh();
        resumeTemplateNotifications();
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
