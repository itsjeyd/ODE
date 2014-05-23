package managers.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.PropertyFunction;
import models.functions.ExistsFunction;
import models.nodes.Rule;


public class RuleManager extends LabeledNodeWithPropertiesManager {

    public RuleManager() {
        this.label = "Rule";
    }

    public Promise<List<Rule>> all() {
        Promise<List<JsonNode>> json = all(this.label);
        return json.map(
            new Function<List<JsonNode>, List<Rule>>() {
                public List<Rule> apply(List<JsonNode> json) {
                    List<Rule> rules = new ArrayList<Rule>();
                    for (JsonNode node: json) {
                        String name = node.get("name").asText();
                        String description = node.get("description")
                            .asText();
                        rules.add(new Rule(name, description));
                    }
                    return rules;

                }
            });
    }

    public Promise<Rule> get(JsonNode properties) {
        Promise<JsonNode> json = get(this.label, properties);
        return json.map(
            new Function<JsonNode, Rule>() {
                public Rule apply(JsonNode json) {
                    String name = json.findValue("name").asText();
                    String description = json.findValue("description")
                        .asText();
                    return new Rule(name, description);
                }
            });
    }




    public static Promise<Boolean> create(Rule rule) {
        rule.jsonProperties.put("description", rule.description);
        return LabeledNodeWithPropertiesManager.create(rule);
    }

    private static Promise<JsonNode> getIncomingRelationships(Rule rule) {
        Promise<WS.Response> response = Neo4jService
            .getIncomingRelationshipsByType(rule.getLabel(),
                                            rule.jsonProperties,
                                            RelationshipType.HAS.name());
        return response.map(new JsonFunction());
    }

    public static Promise<String> getProperty(Rule rule, String propName) {
        Promise<String> ruleURL = Neo4jService
            .getNodeURL(rule.getLabel(), rule.jsonProperties);
        Promise<WS.Response> response = ruleURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String ruleURL) {
                    return Neo4jService.getNodeProperties(ruleURL);
                }
            });
        Promise<JsonNode> json = response.map(new JsonFunction());
        return json.map(new PropertyFunction(propName));
    }

    public static Promise<Boolean> isOrphan(Rule rule) {
        Promise<JsonNode> relationships = RuleManager
            .getIncomingRelationships(rule);
        return relationships.map(
            new Function<JsonNode, Boolean>() {
                public Boolean apply(JsonNode relationships) {
                    return relationships.size() == 0;
                }
            });
    }

    public static Promise<Boolean> has(Rule rule, String string) {
        Promise<WS.Response> response = Neo4jService
            .fuzzyFindTargetsAnyDepth(
                rule, NodeType.OUTPUT_STRING.toString(), "content", string);
        Promise<JsonNode> json = response.map(new JsonFunction());
        return json.map(new ExistsFunction());
    }

    public static Promise<UUID> getUUID(Rule rule) {
        Promise<String> prop = getProperty(rule, "uuid");
        return prop.map(
            new Function<String, UUID>() {
                public UUID apply(String prop) {
                    return UUID.fromString(prop);
                }
            });
    }

}
