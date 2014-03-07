package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.HasValueRelationshipManager;
import models.nodes.Feature;
import models.nodes.Rule;
import models.nodes.Value;


public class HasValueRelationship extends TypedRelationship {
    public Feature startNode;
    public Value endNode;
    public Rule rule;

    public HasValueRelationship(Feature startNode, Value endNode, Rule rule) {
        this.type = RelationshipType.HAS;
        this.startNode = startNode;
        this.endNode = endNode;
        this.rule = rule;
    }

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = HasValueRelationshipManager.get(this);
        return json.map(new ExistsFunction());
    }

    public Promise<Boolean> create() {
        //return this.exists().flatMap(new CreateFunction(this));
        return HasValueRelationshipManager.create(this);
    }

    private class ExistsFunction implements Function<JsonNode, Boolean> {
        public Boolean apply(JsonNode json) {
            return json.get("data").size() > 0;
        }
    }

}
