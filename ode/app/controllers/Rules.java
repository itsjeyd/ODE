package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.libs.F.Function;
import play.libs.F.Promise;

import models.Rule;


public class Rules extends Controller {

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> create() {
        JsonNode json = request().body().asJson();
        final String name = json.findPath("name").textValue();
        final String description = json.findPath("description").textValue();
        Promise<Boolean> created = new Rule(name, description).create();
        return created.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean created) {
                    if (created) {
                        result.put("id", name);
                        result.put("name", name);
                        result.put("description", description);
                        return ok(result);
                    }
                    return badRequest(result);
                }
            });
    }

}
