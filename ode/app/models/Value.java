package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import neo4play.Neo4jService;


public class Value {
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
        return response.map(new CreatedFunction(this));
    }

    public Promise<Boolean> exists() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        Promise<WS.Response> response = dbService
            .getLabeledNodeWithProperties(label, props);
        return response.map(new ExistsFunction());
    }

    private class CreatedFunction implements Function<WS.Response, Value> {
        private Value value;
        public CreatedFunction(Value value) {
            this.value = value;
        }
        public Value apply(WS.Response response) {
            if (response.getStatus() == Status.OK) {
                return this.value;
            }
            return null;
        }
    }

    private class ExistsFunction implements Function<WS.Response, Boolean> {
        public Boolean apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get("data").size() > 0) {
                return true;
            }
            return false;
        }
    }

}
