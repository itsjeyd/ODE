package models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;


public class Value extends Model {
    private static Neo4jService dbService = new Neo4jService();
    protected static String label = "Value";

    public String name;

    public Value(String name) {
        this.name = name;
    }

    public Promise<Value> create() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        Promise<WS.Response> response = dbService
            .createLabeledNodeWithProperties(label, props);
        return response.map(new CreatedFunction<Value>(this));
    }

    public Promise<Boolean> exists() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        Promise<WS.Response> response = dbService
            .getLabeledNodeWithProperties(label, props);
        return response.map(new ExistsFunction());
    }

    public static Promise<List<Value>> all() {
        Promise<WS.Response> response = dbService.getNodesByLabel(label);
        return response.map(new AllFunction());
    }

    private static class AllFunction implements
                                         Function<WS.Response, List<Value>> {
        public List<Value> apply(WS.Response response) {
            List<Value> values = new ArrayList<Value>();
            JsonNode json = response.asJson();
            List<JsonNode> dataNodes = json.findValues("data");
            for (JsonNode dataNode: dataNodes) {
                String name = dataNode.get("name").asText();
                values.add(new Value(name));
            }
            return values;
        }
    }

}
