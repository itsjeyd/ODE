package models.nodes;

import constants.NodeType;
import java.util.List;
import managers.nodes.SlotManager;
import models.relationships.HasPartRelationship;
import play.libs.F.Promise;


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

    protected Promise<List<Part>> getParts() {
        return HasPartRelationship.getEndNodes(this);
    }

}
