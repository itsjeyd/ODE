package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import models.nodes.Feature;
import models.nodes.Rule;
import models.nodes.Value;
import managers.relationships.HasValueRelationshipManager;


public class HasValueRelationship extends HasRelationship {
    public Feature startNode;
    public Value endNode;

    public HasValueRelationship(Feature startNode, Value endNode, Rule rule) {
        super(startNode, endNode, rule);
    }

    public static Promise<Value> getEndNode(
        final Feature startNode, Rule rule) {
        Promise<JsonNode> json = HasValueRelationshipManager.getEndNode(
            startNode, rule);
        return json.map(
            new Function<JsonNode, Value>() {
                public Value apply(JsonNode json) {
                    String name = json.get("data").findValue("name").asText();
                    return new Value(name);
                }
            });
    }

}
