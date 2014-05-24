package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.TypedRelationshipManager;
import models.functions.ExistsFunction;
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

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = TypedRelationshipManager.get(this);
        return json.map(new ExistsFunction());
    }

}
