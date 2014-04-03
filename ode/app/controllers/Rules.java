package controllers;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import models.nodes.CombinationGroup;
import models.nodes.Feature;
import models.nodes.Part;
import models.nodes.Value;
import models.nodes.LHS;
import models.nodes.Rule;
import views.html.input;
import views.html.output;
import views.html.rules;
import views.html.rule;


public class Rules extends Controller {

    @Security.Authenticated(Secured.class)
    public static Promise<Result> rules() {
        Promise<List<Rule>> ruleList = Rule.all();
        return ruleList.map(
            new Function<List<Rule>, Result>() {
                public Result apply(List<Rule> ruleList) {
                    return ok(rules.render(ruleList));
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Result rule(String name) {
        return ok(rule.render("Hi! You are looking at rule " + name + "."));
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> input(final String name) {
        Promise<List<Feature>> globalFeatureList = Feature.all();
        Promise<Rule> rule = new Rule(name).get();
        Promise<Tuple<List<Feature>, Rule>> results = globalFeatureList
            .zip(rule);
        return results.map(
            new Function<Tuple<List<Feature>, Rule>, Result>() {
                public Result apply(Tuple<List<Feature>, Rule> results) {
                    return ok(input.render(results._1, results._2));
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> output(final String name) {
        Promise<List<Part>> globalPartsList = Part.all();
        Promise<Rule> rule = new Rule(name).get();
        Promise<Tuple<List<Part>, Rule>> results = globalPartsList
            .zip(rule);
        return results.map(
            new Function<Tuple<List<Part>, Rule>, Result>() {
                public Result apply(Tuple<List<Part>, Rule> results) {
                    return ok(output.render(results._1, results._2));
                }
            });
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

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateName(final String name) {
        JsonNode json = request().body().asJson();
        final String newName = json.findPath("name").textValue();
        Promise<Boolean> nameAlreadyTaken = new Rule(newName).exists();
        return nameAlreadyTaken.flatMap(
            new Function<Boolean, Promise<Result>>() {
                ObjectNode result = Json.newObject();
                public Promise<Result> apply(Boolean nameAlreadyTaken) {
                    if (nameAlreadyTaken) {
                        return Promise.promise(
                            new Function0<Result>() {
                                public Result apply() {
                                    result.put(
                                        "message", "Name already taken.");
                                    return badRequest(result);
                                }
                            });
                    } else {
                        Promise<Boolean> nameUpdated = new Rule(name)
                            .updateName(newName);
                        return nameUpdated.map(
                            new Function<Boolean, Result>() {
                                public Result apply(Boolean updated) {
                                    if (updated) {
                                        result.put("id", newName);
                                        result.put(
                                            "message",
                                            "Name successfully updated.");
                                        return ok(result);
                                    }
                                    result.put(
                                        "message", "Name not updated.");
                                    return badRequest(result);
                                }
                            });
                    }
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateDescription(String name) {
        JsonNode json = request().body().asJson();
        final String newDescription = json.findPath("description")
            .textValue();
        Promise<Boolean> descriptionUpdated = new Rule(name)
            .updateDescription(newDescription);
        return descriptionUpdated.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        result.put("message",
                                   "Description successfully updated.");
                        return ok(result);
                    }
                    result.put("message",
                               "Description not updated.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addFeature(String name) {
        JsonNode json = request().body().asJson();
        Rule rule = new Rule(name);
        final LHS lhs = new LHS(rule);
        final UUID uuid = UUID.fromString(json.findPath("uuid").textValue());
        final Feature feature = Feature.of(
            json.findPath("name").textValue(),
            json.findPath("type").textValue());
        Promise<Boolean> added = lhs.add(feature, uuid);
        return added.flatMap(
            new Function<Boolean, Promise<Result>>() {
                ObjectNode result = Json.newObject();
                public Promise<Result> apply(Boolean added) {
                    if (added) {
                        Promise<JsonNode> value = lhs
                            .getValue(feature, uuid);
                        return value.map(
                            new Function<JsonNode, Result>() {
                                public Result apply(JsonNode value) {
                                    result.put("value", value);
                                    result.put("message",
                                               "Feature successfully added.");
                                    return ok(result);
                                }
                            });
                    }
                    result.put("message", "Feature not added.");
                    return Promise.promise(
                        new Function0<Result>() {
                            public Result apply() {
                                return badRequest(result);
                            }
                        });
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateFeatureValue(String name) {
        JsonNode json = request().body().asJson();
        Rule rule = new Rule(name);
        LHS lhs = new LHS(rule);
        final UUID uuid = UUID.fromString(json.findPath("uuid").textValue());
        final Feature feature =
            new Feature(json.findPath("name").textValue());
        Value newValue = new Value(json.findPath("newValue").textValue());
        Promise<Boolean> updated = lhs.update(feature, uuid, newValue);
        return updated.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        result.put("message",
                                   "Feature successfully updated.");
                        return ok(result);
                    }
                    result.put("message", "Feature not updated.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeFeature(String name) {
        JsonNode json = request().body().asJson();
        Rule rule = new Rule(name);
        LHS lhs = new LHS(rule);
        final UUID uuid = UUID.fromString(json.findPath("uuid").textValue());
        final Feature feature =
            Feature.of(json.findPath("name").textValue(),
                       json.findPath("type").textValue());
        Promise<Boolean> removed = lhs.remove(feature, uuid);
        return removed.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean removed) {
                    if (removed) {
                        result.put("message",
                                   "Feature successfully removed.");
                        return ok(result);
                    }
                    result.put("message", "Feature not removed.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addString(String name, String groupID) {
        JsonNode json = request().body().asJson();
        String string = json.findPath("string").textValue();
        Promise<Boolean> added = CombinationGroup.of(groupID)
            .addString(string);
        return added.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean added) {
                    if (added) {
                        result.put("message", "String successfully added.");
                        return ok(result);
                    }
                    result.put("message", "String not added.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateString(
        String name, String groupID, String stringID) {
        JsonNode json = request().body().asJson();
        String content = json.findPath("content").textValue();
        Promise<Boolean> updated = CombinationGroup.of(groupID)
            .updateString(stringID, content);
        return updated.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        result.put("message", "String successfully updated.");
                        return ok(result);
                    }
                    result.put("message", "String not updated.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeString(
        String name, String groupID, String stringID) {
        Promise<Boolean> removed = CombinationGroup.of(groupID)
            .removeString(stringID);
        return removed.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean removed) {
                    if (removed) {
                        result.put("message", "String successfully removed.");
                        return ok(result);
                    }
                    result.put("message", "String not removed.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addGroup(String name) {
        JsonNode json = request().body().asJson();
        final UUID uuid = UUID.randomUUID();
        int position = json.findPath("position").intValue();
        CombinationGroup group = new CombinationGroup(uuid, position);
        Promise<Boolean> added = new Rule(name).addGroup(group);
        return added.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean added) {
                    if (added) {
                        result.put("id", uuid.toString());
                        result.put("message", "Group successfully added.");
                        return ok(result);
                    }
                    result.put("message", "Group not added.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeGroup(String name, String groupID) {
        Promise<Boolean> removed = new Rule(name).removeGroup(groupID);
        return removed.map(
        new Function<Boolean, Result>() {
            ObjectNode result = Json.newObject();
            public Result apply(Boolean removed) {
                if (removed) {
                    result.put("message", "Group successfully removed.");
                    return ok(result);
                }
                result.put("message", "Group not removed.");
                return badRequest(result);
            }
        });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addSlot(String name, String groupID) {
        Promise<Boolean> added = CombinationGroup.of(groupID).addSlot();
        return added.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean added) {
                    if (added) {
                        result.put("message", "Slot successfully added.");
                        return ok(result);
                    }
                    result.put("message", "Slot not added.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeSlot(
        String name, String groupID, String slotID) {
        Promise<Boolean> removed = CombinationGroup.of(groupID)
            .removeSlot(slotID);
        return removed.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean removed) {
                    if (removed) {
                        result.put("message", "Slot successfully removed.");
                        return ok(result);
                    }
                    result.put("message", "Slot not removed.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addPart(
        String name, String groupID, String slotID) {
        return null;
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updatePart(
        String name, String groupID, String slotID, String partID) {
        return null;
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removePart(
        String name, String groupID, String slotID, String partID) {
        return null;
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addRef(
        String name, String groupID, String slotID) {
        return null;
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> delete(String name) {
        Promise<Boolean> deleted = new Rule(name).delete();
        return deleted.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean deleted) {
                    if (deleted) {
                        return ok(result);
                    }
                    return badRequest(result);
                }
            });
    }

}
