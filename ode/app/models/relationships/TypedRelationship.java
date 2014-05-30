package models.relationships;

import constants.RelationshipType;
import models.nodes.LabeledNodeWithProperties;


public class TypedRelationship extends Relationship {
    public RelationshipType type;

    private TypedRelationship() {
        super();
    }

    public TypedRelationship(int ID) {
        this.ID = ID;
    }

    protected TypedRelationship(RelationshipType type,
                                LabeledNodeWithProperties startNode,
                                LabeledNodeWithProperties endNode) {
        this();
        this.type = type;
        this.startNode = startNode;
        this.endNode = endNode;
    }

}
