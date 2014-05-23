package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;
import java.util.ArrayList;
import java.util.List;
import managers.functions.NodeListFunction;
import managers.functions.SuccessFunction;
import models.nodes.Foo;
import neo4play.Neo4jService;
import neo4play.NodeService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public abstract class LabeledNodeManager extends NodeManager {

    protected String label;

    public Promise<Boolean> exists(JsonNode properties) {
        return exists(this.label, properties);
    }

    public Promise<List<Foo>> all() {
        Promise<List<JsonNode>> json = all(this.label);
        return json.map(
            new Function<List<JsonNode>, List<Foo>>() {
                public List<Foo> apply(List<JsonNode> json) {
                    List<Foo> foos = new ArrayList<Foo>();
                    for (JsonNode node: json) {
                        String foo = node.get("foo").asText();
                        foos.add(new Foo(foo));
                    }
                    return foos;

                }
            });
    }

    public Promise<Foo> get(JsonNode properties) {
        Promise<JsonNode> json = get(this.label, properties);
        return json.map(
            new Function<JsonNode, Foo>() {
                public Foo apply(JsonNode json) {
                    String foo = json.findValue("foo").asText();
                    return new Foo(foo);
                }
            });
    }

    protected Promise<Boolean> create(JsonNode properties, String location) {
        Promise<WS.Response> response =
            NodeService.createNode(this.label, properties, location);
        return response.map(new SuccessFunction());
    }

    public Promise<Boolean> update(
        JsonNode oldProperties, JsonNode newProperties) {
        return update(this.label, oldProperties, newProperties);
    }

    protected Promise<Boolean> delete(JsonNode properties, String location) {
        Promise<WS.Response> response =
            NodeService.deleteNode(this.label, properties, location);
        return response.map(new SuccessFunction());
    }

    protected static Promise<List<JsonNode>> all(NodeType type) {
        Promise<WS.Response> response = Neo4jService.getNodesByLabel(
            type.toString());
        return response.map(new NodeListFunction());
    }

}
