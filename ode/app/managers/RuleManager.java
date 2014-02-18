package managers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.NodeType;
import models.Rule;
import neo4play.Neo4jService;
import managers.functions.NodeDeletedFunction;
import managers.functions.UpdatedFunction;


public class RuleManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.RULE);
    }

    public static Promise<Boolean> create(Rule rule) {
        rule.jsonProperties.put("description", rule.description);
        return LabeledNodeWithPropertiesManager.create(rule);
    }

    public static Promise<Boolean> updateName(Rule rule, String newName) {
        rule.jsonProperties.put("description", rule.description);
        ObjectNode newProps = rule.jsonProperties.deepCopy()
            .retain("description");
        newProps.put("name", newName);
        Promise<WS.Response> response = Neo4jService.updateNodeProperties(
            rule.label.toString(), rule.jsonProperties, newProps);
        return response.map(new UpdatedFunction());
    }

    public static Promise<Boolean> updateDescription(
        Rule rule, String newDescription) {
        ObjectNode newProps = rule.jsonProperties.deepCopy();
        newProps.put("description", newDescription);
        Promise<WS.Response> response = Neo4jService.updateNodeProperties(
            rule.label.toString(), rule.jsonProperties, newProps);
        return response.map(new UpdatedFunction());
    }

    public static Promise<Boolean> delete(Rule rule) {
        Promise<WS.Response> response = Neo4jService
            .deleteLabeledNodeWithProperties(rule.label.toString(),
                                             rule.jsonProperties);
        return response.map(new NodeDeletedFunction());
    }

}
