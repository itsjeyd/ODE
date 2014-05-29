package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import managers.relationships.HasValueRelationshipManager;
import models.nodes.AVM;
import models.nodes.Feature;
import models.nodes.Rule;
import models.nodes.Value;
import play.libs.F.Function;
import play.libs.F.Promise;


public class HasValueRelationship extends HasRelationship {
    public AVM avm;

    public HasValueRelationship(
        Feature startNode, Value endNode, Rule rule, AVM avm) {
        super(startNode, endNode, rule);
        this.avm = avm;
    }

    public static Promise<Value> getEndNode(
        final Feature startNode, Rule rule, AVM parent) {
        Promise<JsonNode> json = HasValueRelationshipManager.getEndNode(
            startNode, rule, parent);
        return json.map(
            new Function<JsonNode, Value>() {
                public Value apply(JsonNode json) {
                    String name = json.get("data").findValue("name").asText();
                    return new Value(name);
                }
            });
    }

}
