package models.nodes;

import constants.NodeType;
import managers.nodes.CombinationGroupManager;


public class CombinationGroup extends LabeledNodeWithProperties {

    public static final CombinationGroupManager nodes =
        new CombinationGroupManager();

    public CombinationGroup() {
        super(NodeType.COMBINATION_GROUP);
    }

    public CombinationGroup(String uuid) {
        this();
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public CombinationGroup(String uuid, int position) {
        this(uuid);
        this.jsonProperties.put("position", position);
    }

}
