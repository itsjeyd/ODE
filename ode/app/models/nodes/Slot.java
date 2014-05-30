package models.nodes;

import constants.NodeType;
import managers.nodes.SlotManager;


public class Slot extends LabeledNodeWithProperties {

    public static final SlotManager nodes = new SlotManager();

    private Slot() {
        super(NodeType.SLOT);
    }

    public Slot(String uuid) {
        this();
        this.jsonProperties.put("uuid", uuid);
    }

    public Slot(String uuid, int position) {
        this(uuid);
        this.jsonProperties.put("position", position);
    }

}
