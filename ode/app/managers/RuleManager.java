package managers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Promise;

import constants.NodeType;
import models.Rule;


public class RuleManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.RULE);
    }

    public static Promise<Boolean> create(Rule rule) {
        rule.jsonProperties.put("description", rule.description);
        return LabeledNodeWithPropertiesManager.create(rule);
    }

}
