package models.nodes;

import java.util.UUID;

import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.CombinationGroupManager;
import models.relationships.GroupRelationship;


public class CombinationGroup extends LabeledNodeWithProperties {

    public CombinationGroup(UUID uuid, int position) {
        super(NodeType.COMBINATION_GROUP);
        this.jsonProperties.put("uuid", uuid.toString());
        this.jsonProperties.put("position", position);
    }

    public Promise<Boolean> create() {
        return CombinationGroupManager.create(this);
    }

    public Promise<Boolean> connectTo(RHS embeddingRHS) {
        return new GroupRelationship(embeddingRHS, this).create();
    }

    public Promise<Boolean> delete() {
        return Promise.pure(false);
    }

}
