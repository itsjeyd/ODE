package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.JsonFunction;


public abstract class LabeledNodeWithProperties extends LabeledNode {
    public ObjectNode jsonProperties;

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = LabeledNodeWithProperties.Manager.get(this);
        return json.map(new ExistsFunction());
    }


    private class ExistsFunction implements Function<JsonNode, Boolean> {
        public Boolean apply(JsonNode json) {
            return json.get("data").size() > 0;
        }
    }

    public static class Manager {
        private static Neo4jService dbService = new Neo4jService();
        public static Promise<JsonNode> get(LabeledNodeWithProperties node) {
            Promise<WS.Response> response = dbService
                .getLabeledNodeWithProperties(
                    node.label.toString(), node.jsonProperties);
            return response.map(new JsonFunction());
        }
    }

}
