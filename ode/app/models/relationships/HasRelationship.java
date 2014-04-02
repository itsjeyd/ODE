package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Promise;

import managers.relationships.HasRelationshipManager;
import constants.RelationshipType;
import models.functions.ExistsFunction;
import models.nodes.LabeledNodeWithProperties;
import models.nodes.Rule;


public class HasRelationship extends TypedRelationship {
    public Rule rule;

    protected HasRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode,
        Rule rule) {
        super(RelationshipType.HAS, startNode, endNode);
        this.rule = rule;
    }

    @Override
    public Promise<Boolean> exists() {
        Promise<JsonNode> json = HasRelationshipManager.get(this);
        return json.map(new ExistsFunction());
    }

    public Promise<Boolean> create() {
        return HasRelationshipManager.create(this);
    }

}
