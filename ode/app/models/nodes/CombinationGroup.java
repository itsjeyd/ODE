package models.nodes;

import java.util.UUID;

import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.CombinationGroupManager;
import models.relationships.GroupRelationship;


public class CombinationGroup extends LabeledNodeWithProperties {

    private CombinationGroup(UUID uuid) {
        super(NodeType.COMBINATION_GROUP);
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public CombinationGroup(UUID uuid, int position) {
        super(NodeType.COMBINATION_GROUP);
        this.jsonProperties.put("uuid", uuid.toString());
        this.jsonProperties.put("position", position);
    }

    public static CombinationGroup of(String groupID) {
        return new CombinationGroup(UUID.fromString(groupID));
    }

    public Promise<Boolean> create() {
        return CombinationGroupManager.create(this);
    }

    public Promise<Boolean> connectTo(RHS embeddingRHS) {
        return new GroupRelationship(embeddingRHS, this).create();
    }

    public Promise<Boolean> addString(OutputString string) {
        return string.connectTo(this);
    }

    public Promise<Boolean> removeString(OutputString string) {
        return string.removeFrom(this);
    }

    public Promise<Boolean> addSlot(Slot slot) {
        return slot.connectTo(this);
    }

    public Promise<Boolean> removeSlot(Slot slot) {
        return slot.removeFrom(this);
    }

    public Promise<Boolean> addRef(String slotID, String ruleName) {
        return null;
    }

    public Promise<Boolean> delete() {
        return Promise.pure(false);
    }

}
