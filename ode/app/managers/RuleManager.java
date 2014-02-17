package managers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.NodeType;
import models.Rule;
import neo4play.Neo4jService;
import managers.functions.NodeDeletedFunction;


public class RuleManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.RULE);
    }

    public static Promise<Boolean> create(Rule rule) {
        rule.jsonProperties.put("description", rule.description);
        return LabeledNodeWithPropertiesManager.create(rule);
    }

    public static Promise<Boolean> delete(Rule rule) {
        Promise<WS.Response> response = Neo4jService
            .deleteLabeledNodeWithProperties(rule.label.toString(),
                                             rule.jsonProperties);
        return response.map(new NodeDeletedFunction());
    }

}
