package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.NodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import managers.functions.JsonFunction;
import managers.functions.PropertyFunction;
import models.functions.ExistsFunction;
import models.nodes.LHS;
import models.nodes.RHS;
import models.nodes.Rule;
import models.relationships.Has;
import neo4play.Neo4jService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.WS;
import utils.UUIDGenerator;


public class RuleManager extends LabeledNodeWithPropertiesManager {

    public RuleManager() {
        this.label = "Rule";
    }

    // READ

    @Override
    public Promise<Boolean> exists(JsonNode properties) {
        return super.exists(properties, "name");
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

    // CREATE

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
                        LHS lhs = new LHS(uuid);
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

    // DELETE

    @Override
    protected Promise<Boolean> delete(
        final JsonNode properties, final String location) {
        // 1. Remove LHS
        Promise<Boolean> deleted = removeLHS(properties, location);
        // 2. Remove RHS
        deleted = deleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean deleted) {
                    if (deleted) {
                        return RuleManager.this
                            .removeRHS(properties, location);
                    }
                    return Promise.pure(false);
                }
            });
        // 3. Delete rule
        deleted = deleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean deleted) {
                    if (deleted) {
                        return RuleManager.super.delete(properties, location);
                    }
                    return Promise.pure(false);
                }
            });
        return deleted;
    }

    private Promise<Boolean> removeLHS(
        JsonNode properties, final String location) {
        final String ruleUUID = properties.get("uuid").asText();
        String uuid = UUIDGenerator.from(ruleUUID);
        final LHS lhs = new LHS(uuid);
        Promise<Boolean> removed = disconnect(properties, lhs, location);
        removed = removed.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean removed) {
                    if (removed) {
                        ObjectNode props = lhs.getProperties().deepCopy();
                        props.put("ruleUUID", ruleUUID);
                        return LHS.nodes.delete(props, location);
                    }
                    return Promise.pure(false);
                }
            });
        return removed;
    }

    private Promise<Boolean> removeRHS(
        JsonNode properties, final String location) {
        String ruleUUID = properties.get("uuid").asText();
        String uuid = UUIDGenerator.from(ruleUUID);
        final RHS rhs = new RHS(uuid);
        Promise<Boolean> removed = disconnect(properties, rhs, location);
        removed = removed.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean removed) {
                    if (removed) {
                        return RHS.nodes
                            .delete(rhs.getProperties(), location);
                    }
                    return Promise.pure(false);
                }
            });
        return removed;
    }

    private Promise<Boolean> disconnect(
        JsonNode properties, LHS lhs, String location) {
        Rule rule = new Rule(properties.get("name").asText());
        return models.relationships
            .LHS.relationships.delete(rule, lhs, location);
    }

    private Promise<Boolean> disconnect(
        JsonNode properties, RHS rhs, String location) {
        Rule rule = new Rule(properties.get("name").asText());
        return models.relationships
            .RHS.relationships.delete(rule, rhs, location);
    }

    // Connections to other nodes

    public Promise<Boolean> orphaned(JsonNode properties) {
        Rule rule = new Rule(properties.get("name").asText());
        return super.orphaned(rule, Has.relationships);
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
