package managers.nodes;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.JsonFunction;
import managers.functions.NodeCreatedFunction;
import managers.functions.NodeDeletedFunction;
import managers.functions.UpdatedFunction;
import models.nodes.LabeledNodeWithProperties;
import models.nodes.Node;
import neo4play.Neo4jService;


public class LabeledNodeWithPropertiesManager extends LabeledNodeManager {

    public Promise<? extends List<? extends Node>> all() {
        return null;
    }

    public Promise<? extends Node> get(JsonNode properties) {
        return null;
    }


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

    public static Promise<Boolean> delete(LabeledNodeWithProperties node) {
        Promise<WS.Response> response = Neo4jService
            .deleteLabeledNodeWithProperties(node.getLabel(),
                                             node.jsonProperties);
        return response.map(new NodeDeletedFunction());
    }

    public static Promise<Boolean> updateProperties(
        LabeledNodeWithProperties node, JsonNode newProps) {
        Promise<WS.Response> response = Neo4jService.updateNodeProperties(
            node.getLabel(), node.jsonProperties, newProps);
        return response.map(new UpdatedFunction());
    }

}
