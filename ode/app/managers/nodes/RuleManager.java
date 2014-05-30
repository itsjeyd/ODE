package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import models.nodes.Feature;
import models.nodes.LHS;
import models.nodes.RHS;
import models.nodes.Rule;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.Json;
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
                    String uuid = json.findValue("uuid").asText();
                    return new Rule(name, description, uuid);
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

    // Custom functionality

    public Promise<Rule> full(JsonNode properties) {
        Promise<Rule> rule = get(properties);
        rule = rule.flatMap(
            new Function<Rule, Promise<Rule>>() {
                public Promise<Rule> apply(final Rule rule) {
                    ObjectNode properties = Json.newObject();
                    String uuid = UUIDGenerator.from(rule.uuid);
                    properties.put("uuid", uuid);
                    Promise<RHS> rhs = RHS.nodes.get(properties);
                    properties.put("ruleUUID", rule.uuid);
                    Promise<LHS> lhs = LHS.nodes.get(properties);
                    return lhs.zip(rhs).map(
                        new Function<Tuple<LHS, RHS>, Rule>() {
                            public Rule apply(Tuple<LHS, RHS> components) {
                                rule.lhs = components._1;
                                rule.rhs = components._2;
                                return rule;
                            }
                        });
                }
            });
        return rule;
    }

    public Set<Rule> from(List<JsonNode> ruleNodes) {
        Set<Rule> rules = new HashSet<Rule>();
        for (JsonNode ruleNode: ruleNodes) {
            String name = ruleNode.findValue("name").asText();
            String description = ruleNode.findValue("description").asText();
            Rule rule = new Rule(name, description);
            String uuid = ruleNode.findValue("uuid").asText();
            rule.setUUID(uuid);
            rules.add(rule);
        }
        return rules;
    }

    public Promise<Set<Rule>> matching(final JsonNode strings) {
        Promise<List<Rule>> ruleList = all(this.label).map(
            new Function<List<JsonNode>, List<Rule>>() {
                public List<Rule> apply(List<JsonNode> nodes) {
                    List<Rule> rules = new ArrayList<Rule>();
                    for (JsonNode node: nodes) {
                        String name = node.get("name").asText();
                        String description = node.get("description").asText();
                        Rule rule = new Rule(name, description);
                        String uuid = node.get("uuid").asText();
                        rule.setUUID(uuid);
                        rules.add(rule);
                    }
                    return rules;

                }
            });
        Promise<Set<Rule>> rules = ruleList.map(
            new Function<List<Rule>, Set<Rule>>() {
                public Set<Rule> apply(List<Rule> ruleList) {
                    Set<Rule> rules = new HashSet<Rule>();
                    rules.addAll(ruleList);
                    return rules;
                }
            });
        return rules.flatMap(
            new Function<Set<Rule>, Promise<Set<Rule>>>() {
                public Promise<Set<Rule>> apply(Set<Rule> rules) {
                    return matching(rules, strings);
                }
            });
    }

    public Promise<Set<Rule>> matching(Set<Rule> rules, JsonNode strings) {
        List<Promise<? extends Rule>> matchingRules =
            new ArrayList<Promise<? extends Rule>>();
        for (final Rule rule: rules) {
            JsonNode properties = rule.getProperties();
            Promise<Boolean> hasStrings = has(properties, strings);
            Promise<Rule> matchingRule = hasStrings.map(
                new Function<Boolean, Rule>() {
                    public Rule apply(Boolean hasStrings) {
                        if (hasStrings) {
                            return rule;
                        }
                        return null;
                    }
                });
            matchingRules.add(matchingRule);
        }
        return Promise.sequence(matchingRules).map(
            new Function<List<Rule>, Set<Rule>>() {
                public Set<Rule> apply(List<Rule> matchingRules) {
                    Set<Rule> result = new HashSet<Rule>();
                    for (Rule matchingRule: matchingRules) {
                        if (matchingRule != null) {
                            result.add(matchingRule);
                        }
                    }
                    return result;
                }
            });
    }

    private Promise<Boolean> has(
        final JsonNode properties, JsonNode strings) {
        String ruleUUID = properties.get("uuid").asText();
        String uuid = UUIDGenerator.from(ruleUUID);
        ObjectNode props = Json.newObject();
        props.put("uuid", uuid);
        return RHS.nodes.find(props, strings);
    }

    public Promise<List<Rule>> similar(JsonNode properties) {
        Promise<JsonNode> json = get(this.label, properties);
        Promise<List<JsonNode>> features = json.flatMap(
            new Function<JsonNode, Promise<List<JsonNode>>>() {
                public Promise<List<JsonNode>> apply(JsonNode json) {
                    String ruleUUID = json.findValue("uuid").asText();
                    String uuid = UUIDGenerator.from(ruleUUID);
                    ObjectNode props = Json.newObject();
                    props.put("ruleUUID", ruleUUID);
                    props.put("uuid", uuid);
                    return LHS.nodes.features(props);
                }
            });
        Promise<Set<Rule>> similarRules = features.flatMap(
            new Function<List<JsonNode>, Promise<Set<Rule>>>() {
                public Promise<Set<Rule>> apply(List<JsonNode> features) {
                    List<Promise<? extends Set<Rule>>> ruleSets =
                        new ArrayList<Promise<? extends Set<Rule>>>();
                    for (JsonNode feature: features) {
                        ruleSets.add(Feature.nodes.rules(feature));
                    }
                    return Promise.sequence(ruleSets)
                        .map(new IntersectFunction());
                }
            });
        return similarRules.map(
            new Function<Set<Rule>, List<Rule>>() {
                public List<Rule> apply(Set<Rule> similarRules) {
                    List<Rule> rules = new ArrayList<Rule>();
                    rules.addAll(similarRules);
                    return rules;
                }
            });
    }

    private static class IntersectFunction
        implements Function<List<Set<Rule>>, Set<Rule>> {
        public Set<Rule> apply(List<Set<Rule>> ruleSets) {
            Set<Rule> rules = new HashSet<Rule>();
            boolean firstSet = true;
            for (Set<Rule> ruleSet: ruleSets) {
                if (firstSet) {
                    rules.addAll(ruleSet);
                    firstSet = false;
                } else {
                    rules.retainAll(ruleSet);
                }
            }
            return rules;
        }
    }

}
