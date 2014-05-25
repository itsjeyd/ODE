package managers.nodes;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import models.nodes.LHS;
import models.nodes.RHS;
import play.libs.Json;

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
import utils.UUIDGenerator;


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

    @Override
    protected Promise<Boolean> create(
        JsonNode properties, final String location) {
        final Rule rule = new Rule(properties.get("name").asText());
        // 1. Generate UUIDs for rule, LHS, and RHS
        final String ruleUUID = UUIDGenerator.random();
        final String uuid = UUIDGenerator.from(ruleUUID);
        // 2. Create rule
        ObjectNode props = (ObjectNode) properties.deepCopy();
        props.put("uuid", ruleUUID);
        Promise<Boolean> created = super.create(props, location, "name");
        // 3. Create LHS
        created = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        return LHS.nodes.create(
                            Json.newObject().put("uuid", uuid), location);
                    }
                    return Promise.pure(false);
                }
            });
        // 4. Connect rule to LHS
        created = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        LHS lhs = new LHS(rule, uuid);
                        return models.relationships.LHS.relationships
                            .create(rule, lhs, location);
                    }
                    return Promise.pure(false);
                }
            });
        // 5. Create RHS
        created = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean lhsCreated) {
                    if (lhsCreated) {
                        return RHS.nodes.create(
                            Json.newObject().put("uuid", uuid), location);
                    }
                    return Promise.pure(false);
                }
            });
        // 6. Connect rule to RHS
        created = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        RHS rhs = new RHS(rule, uuid);
                        return models.relationships.RHS.relationships
                            .create(rule, rhs, location);
                    }
                    return Promise.pure(false);
                }
            });
        return created;
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
