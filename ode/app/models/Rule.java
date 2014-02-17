package models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.RuleManager;


public class Rule extends LabeledNodeWithProperties {
    public String name;
    public String description;

    private Rule() {
        this.label = NodeType.RULE;
        this.jsonProperties = Json.newObject();
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

    public static Promise<List<Rule>> all() {
        Promise<List<JsonNode>> json = RuleManager.all();
        return json.map(new AllFunction());
    }

    public Promise<Rule> get() {
        Promise<JsonNode> json = RuleManager.get(this);
        return json.map(new GetFunction());
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    public Promise<Boolean> delete() {
        return RuleManager.delete(this);
    }

    private static class GetFunction implements
                                         Function<JsonNode, Rule> {
        public Rule apply(JsonNode json) {
            String name = "@" + json.findValue("name").asText();
            String description = json.findValue("description").asText();
            return new Rule(name, description);
        }
    }

    private static class AllFunction implements
                                  Function<List<JsonNode>, List<Rule>> {
        public List<Rule> apply(List<JsonNode> dataNodes) {
            List<Rule> rules = new ArrayList<Rule>();
            for (JsonNode dataNode: dataNodes) {
                String name = "@" + dataNode.get("name").asText();
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
            return RuleManager.create(rule);
        }
    }
}
