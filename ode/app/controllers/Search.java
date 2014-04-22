package controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import views.html.search;


public class Search extends Controller {

    @Security.Authenticated(Secured.class)
    public static Result search() {
        return ok(search.render());
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Result doSearch() {
        ObjectNode result = Json.newObject();
        ArrayNode matchingRules = JsonNodeFactory.instance.arrayNode();
        ObjectNode dummyRule = Json.newObject();
        dummyRule.put("name", "rule1");
        dummyRule.put("description", "The first rule of fight club is...");
        matchingRules.add(dummyRule);
        result.put("matchingRules", matchingRules);
        return ok(result);
    }

}
