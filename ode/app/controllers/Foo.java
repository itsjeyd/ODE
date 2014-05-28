package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.libs.F.Promise;


public class Foo extends Controller {

    public static Promise<Result> foo() {
        // ObjectNode oldProperties = Json.newObject();
        // oldProperties.put("foo", "bar");
        // ObjectNode newProperties = Json.newObject();
        // newProperties.put("foo", "baz");
        // ObjectNode properties = Json.newObject();
        // properties.put("hi", "ho");
        models.nodes.Foo bar = new models.nodes.Foo("bar");
        models.nodes.Foo baz = new models.nodes.Foo("baz");
        // Promise<Boolean> deleted = models.nodes.Foo.nodes
        //     .delete(properties);
        Promise<List<JsonNode>> endNodes = models.relationships.Bar
            .relationships.endNodes(bar);
        return endNodes.map(
            new Function<List<JsonNode>, Result>() {
                public Result apply(List<JsonNode> endNodes) {
                    // if (exists) {
                    //     return ok(foo.render("Bar exists."));
                    // }
                    // return ok(foo.render("Bar does not exist."));
                    return ok(foo.render(endNodes.toString()));
                }
            });
    }

}
