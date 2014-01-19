package managers;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.JsonFunction;
import models.LabeledNodeWithProperties;
import neo4play.Neo4jService;


public class LabeledNodeWithPropertiesManager {

    public static Promise<JsonNode> get(LabeledNodeWithProperties node) {
        Promise<WS.Response> response = Neo4jService
            .getLabeledNodeWithProperties(
                node.label.toString(), node.jsonProperties);
        return response.map(new JsonFunction());
    }

}
