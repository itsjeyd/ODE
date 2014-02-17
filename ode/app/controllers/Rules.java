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
import views.html.rules;
import views.html.rule;


public class Rules extends Controller {

    @Security.Authenticated(Secured.class)
    public static Result rules() {
        return ok(rules.render("Hi! This is Ode's Rule Browser."));
    }

    @Security.Authenticated(Secured.class)
    public static Result rule(String name) {
        return ok(rule.render("Hi! You are looking at rule " + name + "."));
    }

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
