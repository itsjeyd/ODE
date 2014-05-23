package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import models.nodes.Foo;
import play.libs.F.Function;
import play.libs.F.Promise;


public class FooManager extends LabeledNodeManager {

    public FooManager() {
        this.label = "Foo";
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

}
