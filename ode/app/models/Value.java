package models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import neo4play.Neo4jService;
import managers.functions.NodeCreatedFunction;
import managers.functions.NodeListFunction;


public class Value extends OntologyNode {
    private Value() {
        this.label = NodeType.VALUE;
        this.jsonProperties = Json.newObject();
    }

    public Value(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public static Promise<List<Value>> all() {
        Promise<List<JsonNode>> json = Value.Manager.all();
        return json.map(new AllFunction());
    }


    private static class AllFunction
        implements Function<List<JsonNode>, List<Value>> {
        public List<Value> apply(List<JsonNode> dataNodes) {
            List<Value> values = new ArrayList<Value>();
            for (JsonNode dataNode: dataNodes) {
                String name = dataNode.get("name").asText();
                values.add(new Value(name));
            }
            return values;
        }
    }


    public static class Manager {
        private static Neo4jService dbService = new Neo4jService();
        public static Promise<List<JsonNode>> all() {
            Promise<WS.Response> response = dbService.getNodesByLabel(
                NodeType.VALUE.toString());
            return response.map(new NodeListFunction());
        }
        public static Promise<Boolean> create(Value value) {
            Promise<WS.Response> response = dbService
                .createLabeledNodeWithProperties(
                    value.label.toString(), value.jsonProperties);
            return response.map(new NodeCreatedFunction());
        }
    }

}
