package models.nodes;

import constants.NodeType;
import managers.nodes.RuleManager;
import models.nodes.RHS;


public class Rule extends LabeledNodeWithProperties {

    public static final RuleManager nodes = new RuleManager();

    public String name;
    public String description;
    public LHS lhs;
    public RHS rhs;
    public String uuid;

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

    public Rule(String name, String description, String uuid) {
        this(name, description);
        this.uuid = uuid;
        this.jsonProperties.put("uuid", uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rule)) {
            return false;
        }
        Rule that = (Rule) o;
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = 17;
        int c = this.name == null ? 0 : this.name.hashCode();
        result = 31 * result + c;
        return result;
    }

}
