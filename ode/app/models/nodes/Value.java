package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import constants.NodeType;
import managers.nodes.ValueManager;
import play.libs.F.Promise;


public class Value extends OntologyNode {

    public static final ValueManager nodes = new ValueManager();

    private Value() {
        super(NodeType.VALUE);
    }

    public Value(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Promise<JsonNode> toJSON() {
        JsonNode node = new TextNode(this.name);
        return Promise.pure(node);
    }

}
