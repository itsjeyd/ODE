package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.RuleManager;
import models.nodes.RHS;
import models.relationships.LHSRelationship;
import models.relationships.RHSRelationship;


public class Rule extends UUIDNode {
    public String name;
    public String description;
    public LHS lhs;

    private Rule() {
        super(NodeType.RULE);
    }

    public Rule(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Rule(String name, String description) {
        this(name);
        this.description = description;
    }

    public Promise<UUID> getUUID() {
        return RuleManager.getUUID(this);
    }

    public static Promise<List<Rule>> all() {
        Promise<List<JsonNode>> json = RuleManager.all();
        return json.map(new AllFunction());
    }

    public Promise<Rule> get() {
        Promise<JsonNode> json = RuleManager.get(this);
        return json.flatMap(new GetFunction());
    }

    public Promise<Boolean> create() {
        Promise<Boolean> created = this.exists()
            .flatMap(new CreateFunction(this));
        Promise<Boolean> lhsCreated = created
            .flatMap(new CreateLHSFunction(this));
        return lhsCreated.flatMap(new CreateRHSFunction(this));
    }

    public Promise<Boolean> updateName(final String newName) {
        Promise<Rule> rule = this.get();
        return rule.flatMap(
            new Function<Rule, Promise<Boolean>>() {
                public Promise<Boolean> apply(Rule rule) {
                    return RuleManager.updateName(rule, newName);
                }
            });
    }

    public Promise<Boolean> updateDescription(final String newDescription) {
        Promise<Rule> rule = this.get();
        return rule.flatMap(
            new Function<Rule, Promise<Boolean>>() {
                public Promise<Boolean> apply(Rule rule) {
                    return RuleManager.updateDescription(
                        rule, newDescription);
                }
            });
    }

    public Promise<Boolean> updateLHS(Feature feature) {
        LHS lhs = new LHS(this);
        return lhs.add(feature);
    }

    public Promise<Boolean> delete() {
        final Rule rule = this;
        final LHS lhs = new LHS(rule);
        final RHS rhs = new RHS(rule);
        Promise<Boolean> emptied = lhs.empty();
        Promise<Boolean> lhsRelationshipDeleted = emptied.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean emptied) {
                    if (emptied) {
                        return LHSRelationship.delete(rule, lhs);
                    }
                    return Promise.pure(false);
                }
            });
        Promise<Boolean> lhsDeleted = lhsRelationshipDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(
                    Boolean lhsRelationshipDeleted) {
                    if (lhsRelationshipDeleted) {
                        return lhs.delete();
                    }
                    return Promise.pure(false);
                }
            });
        Promise<Boolean> rhsRelationshipDeleted = lhsDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean lhsDeleted) {
                    if (lhsDeleted) {
                        return RHSRelationship.delete(rule, rhs);
                    }
                    return Promise.pure(false);
                }
            });
        Promise<Boolean> rhsDeleted = rhsRelationshipDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(
                    Boolean rhsRelationshipDeleted) {
                    if (rhsRelationshipDeleted) {
                        return rhs.delete();
                    }
                    return Promise.pure(false);
                }
            });
        return rhsDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean rhsDeleted) {
                    if (rhsDeleted) {
                        return RuleManager.delete(rule);
                    }
                    return Promise.pure(false);
                }
            });
    }

    private static class GetFunction implements
                                         Function<JsonNode, Promise<Rule>> {
        public Promise<Rule> apply(JsonNode json) {
            String name = json.findValue("name").asText();
            String description = json.findValue("description").asText();
            String uuid = json.findValue("uuid").asText();
            Rule rule = new Rule(name, description);
            rule.jsonProperties.put("uuid", uuid);
            Promise<LHS> lhs = new LHS(rule).get();
            return lhs.map(new SetLHSFunction(rule));
        }
    }

    private static class SetLHSFunction implements Function<LHS, Rule> {
        private Rule rule;
        public SetLHSFunction(Rule rule) {
            this.rule = rule;
        }
        public Rule apply(LHS lhs) {
            this.rule.lhs = lhs;
            return this.rule;
        }
    }

    private static class AllFunction implements
                                  Function<List<JsonNode>, List<Rule>> {
        public List<Rule> apply(List<JsonNode> dataNodes) {
            List<Rule> rules = new ArrayList<Rule>();
            for (JsonNode dataNode: dataNodes) {
                String name = dataNode.get("name").asText();
                String description = dataNode.get("description").asText();
                rules.add(new Rule(name, description));
            }
            return rules;
        }
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private Rule rule;
        public CreateFunction(Rule rule) {
            this.rule = rule;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            rule.jsonProperties.put("uuid", UUID.randomUUID().toString());
            return RuleManager.create(rule);
        }
    }

    private class CreateLHSFunction implements
                                        Function<Boolean, Promise<Boolean>> {

        private Rule rule;
        public CreateLHSFunction(Rule rule) {
            this.rule = rule;
        }
        public Promise<Boolean> apply(Boolean created) {
            if (!created) {
                return Promise.pure(false);
            }
            final LHS lhs = new LHS(this.rule);
            Promise<Boolean> lhsCreated = lhs.create();
            final Rule rule = this.rule;
            return lhsCreated.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean created) {
                        if (created) {
                            return lhs.connectTo(rule);
                        }
                        return Promise.pure(false);
                    }
                });
        }
    }

    private class CreateRHSFunction implements
                                        Function<Boolean, Promise<Boolean>> {

        private Rule rule;
        public CreateRHSFunction(Rule rule) {
            this.rule = rule;
        }
        public Promise<Boolean> apply(Boolean lhsCreated) {
            if (!lhsCreated) {
                return Promise.pure(false);
            }
            final RHS rhs = new RHS(this.rule);
            Promise<Boolean> rhsCreated = rhs.create();
            final Rule rule = this.rule;
            return rhsCreated.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean created) {
                        if (created) {
                            return rhs.connectTo(rule);
                        }
                        return Promise.pure(false);
                    }
                });
        }
    }

}
