package managers;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.JsonFunction;
import models.LabeledNodeWithProperties;
import neo4play.Neo4jService;


public class LabeledNodeWithPropertiesManager {

    private static Neo4jService dbService = new Neo4jService();

    public static Promise<JsonNode> get(LabeledNodeWithProperties node) {
        Promise<WS.Response> response = dbService
            .getLabeledNodeWithProperties(
                node.label.toString(), node.jsonProperties);
        return response.map(new JsonFunction());
    }

}
