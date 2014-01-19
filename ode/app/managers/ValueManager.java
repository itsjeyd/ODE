package managers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.NodeType;
import managers.functions.NodeCreatedFunction;
import managers.functions.NodeListFunction;
import models.Value;
import neo4play.Neo4jService;


public class ValueManager {

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
