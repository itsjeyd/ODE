package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import managers.functions.SuccessFunction;
import models.nodes.Foo;
import neo4play.NodeService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public class FooManager extends NodeManager {

    public Promise<Boolean> exists(JsonNode properties) {
        return exists("Foo", properties);
    }

    public Promise<List<Foo>> all() {
        Promise<List<JsonNode>> json = all("Foo");
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
        Promise<JsonNode> json = get("Foo", properties);
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
            NodeService.createNode("Foo", properties, location);
        return response.map(new SuccessFunction());
    }

    public Promise<Boolean> update(
        JsonNode oldProperties, JsonNode newProperties) {
        return update("Foo", oldProperties, newProperties);
    }

    protected Promise<Boolean> delete(JsonNode properties, String location) {
        Promise<WS.Response> response =
            NodeService.deleteNode("Foo", properties, location);
        return response.map(new SuccessFunction());
    }

}
