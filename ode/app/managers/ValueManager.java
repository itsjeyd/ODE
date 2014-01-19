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

    public static Promise<List<JsonNode>> all() {
        Promise<WS.Response> response = Neo4jService.getNodesByLabel(
            NodeType.VALUE.toString());
        return response.map(new NodeListFunction());
    }

    public static Promise<Boolean> create(Value value) {
        Promise<WS.Response> response = Neo4jService
            .createLabeledNodeWithProperties(
                value.label.toString(), value.jsonProperties);
        return response.map(new NodeCreatedFunction());
    }

}
