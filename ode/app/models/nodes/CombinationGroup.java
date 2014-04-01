package models.nodes;

import java.util.UUID;

import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.CombinationGroupManager;
import models.relationships.GroupRelationship;


public class CombinationGroup extends LabeledNodeWithProperties {

    public CombinationGroup() {
        super(NodeType.COMBINATION_GROUP);
    }

    public Promise<Boolean> create() {
        UUID uuid = UUID.randomUUID();
        this.jsonProperties.put("uuid", uuid.toString());
        return CombinationGroupManager.create(this);
    }

    public Promise<Boolean> connectTo(RHS embeddingRHS) {
        return new GroupRelationship(embeddingRHS, this).create();
    }

}
