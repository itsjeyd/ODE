package managers.nodes;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import models.nodes.Value;


public class ValueManager extends LabeledNodeWithPropertiesManager {

    public ValueManager() {
        this.label = "Value";
    }

    public Promise<List<Value>> all() {
        Promise<List<JsonNode>> json = all(this.label);
        return json.map(
            new Function<List<JsonNode>, List<Value>>() {
                public List<Value> apply(List<JsonNode> json) {
                    List<Value> values = new ArrayList<Value>();
                    for (JsonNode node: json) {
                        String name = node.get("name").asText();
                        if (!name.equals("underspecified")) {
                            values.add(new Value(name));
                        }
                    }
                    return values;
                }
            });
    }


    public static Promise<JsonNode> getIncomingRelationships(Value value) {
        Promise<WS.Response> response = Neo4jService
            .getIncomingRelationshipsByType(value.getLabel(),
                                            value.jsonProperties,
                                            RelationshipType.ALLOWS.name());
        return response.map(new JsonFunction());
    }

}
