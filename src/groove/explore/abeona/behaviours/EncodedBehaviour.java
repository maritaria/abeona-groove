package groove.explore.abeona.behaviours;

import abeona.behaviours.ExplorationBehaviour;
import groove.explore.encode.EncodedType;
import groove.lts.GraphState;

public interface EncodedBehaviour extends EncodedType<ExplorationBehaviour<GraphState>, String> {
    String getEncodingKeyword();

    String getDisplayLabel();
}
