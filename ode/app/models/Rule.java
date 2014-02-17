package models;

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

    private Rule(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Rule(String name, String description) {
        this(name);
        this.description = description;
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
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
