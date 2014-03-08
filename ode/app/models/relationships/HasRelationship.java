package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import managers.relationships.HasRelationshipManager;
import constants.RelationshipType;
import models.nodes.LabeledNodeWithProperties;
import models.nodes.Rule;


public class HasRelationship extends TypedRelationship {
    public LabeledNodeWithProperties startNode;
    public LabeledNodeWithProperties endNode;
    public Rule rule;

    private HasRelationship() {
        this.type = RelationshipType.HAS;
    }

    protected HasRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode,
        Rule rule) {
        this();
        this.startNode = startNode;
        this.endNode = endNode;
        this.rule = rule;
    }

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = HasRelationshipManager.get(this);
        return json.map(new ExistsFunction());
    }

    public Promise<Boolean> create() {
        return HasRelationshipManager.create(this);
    }

    private class ExistsFunction implements Function<JsonNode, Boolean> {
        public Boolean apply(JsonNode json) {
            return json.get("data").size() > 0;
        }
    }

}
