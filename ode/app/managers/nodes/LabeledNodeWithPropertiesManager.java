package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.JsonFunction;
import managers.functions.NodeCreatedFunction;
import models.nodes.LabeledNodeWithProperties;
import neo4play.Neo4jService;


public class LabeledNodeWithPropertiesManager extends LabeledNodeManager {

    public static Promise<JsonNode> get(LabeledNodeWithProperties node) {
        Promise<WS.Response> response = Neo4jService
            .getLabeledNodeWithProperties(
                node.getLabel(), node.jsonProperties);
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> create(LabeledNodeWithProperties node) {
        Promise<WS.Response> response = Neo4jService
            .createLabeledNodeWithProperties(
                node.getLabel(), node.jsonProperties);
        return response.map(new NodeCreatedFunction());
    }

}
