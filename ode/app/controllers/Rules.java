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
import models.nodes.OutputString;
import models.nodes.Part;
import models.nodes.Value;
import models.nodes.LHS;
import models.nodes.RHS;
import models.nodes.Rule;
import models.nodes.Slot;
import views.html.browse;
import views.html.details;
import views.html.input;
import views.html.output;


public class Rules extends Controller {

    @Security.Authenticated(Secured.class)
    public static Promise<Result> browse() {
        Promise<List<Rule>> ruleList = Rule.nodes.all();
        return ruleList.map(
            new Function<List<Rule>, Result>() {
                public Result apply(List<Rule> ruleList) {
                    return ok(browse.render(ruleList));
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> details(String name) {
        Promise<Rule> requestedRule = new Rule(name).get();
        return requestedRule.map(
            new Function<Rule, Result>() {
                public Result apply(Rule requestedRule) {
                    return ok(details.render(requestedRule));
                }
            });
    }


    @Security.Authenticated(Secured.class)
    public static Promise<Result> similar(String name) {
        Rule rule = new Rule(name);
        Promise<List<Rule>> ruleList = rule.getSimilarRules();
        return ruleList.map(
            new Function<List<Rule>, Result>() {
                public Result apply(List<Rule> ruleList) {
                    return ok(browse.render(ruleList));
                }
            });
    }


    @Security.Authenticated(Secured.class)
    public static Promise<Result> input(final String name) {
        Promise<List<Feature>> globalFeatureList = Feature.nodes.all();
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
        Promise<List<Part>> globalPartsList = Part.nodes.all();
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
    public static Promise<Result> lhs(String name) {
        Rule rule = new Rule(name);
        Promise<JsonNode> lhsJSON = new LHS(rule).toJSON();
        return lhsJSON.map(
            new Function<JsonNode, Result>() {
                public Result apply(JsonNode lhsJSON) {
                    ObjectNode result = Json.newObject();
                    result.put("json", lhsJSON);
                    return ok(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> rhs(String name) {
        Rule rule = new Rule(name);
        Promise<JsonNode> rhsJSON = new RHS(rule).toJSON();
        return rhsJSON.map(
            new Function<JsonNode, Result>() {
                public Result apply(JsonNode rhsJSON) {
                    ObjectNode result = Json.newObject();
                    result.put("json", rhsJSON);
                    return ok(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> create() {
        final JsonNode json = request().body().asJson();
        Promise<Boolean> created = Rule.nodes.create(json);
        return created.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean created) {
                    if (created) {
                        String name = json.get("name").asText();
                        result.put("id", name);
                        result.put("name", name);
                        result.put(
                            "description", json.get("description").asText());
                        return ok(result);
                    }
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateName(final String name) {
        final ObjectNode newProps = (ObjectNode) request().body().asJson();
        Promise<Boolean> nameTaken =
            Rule.nodes.exists(newProps.deepCopy().retain("name"));
        Promise<Boolean> updated = nameTaken.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean nameTaken) {
                    if (nameTaken) {
                        return Promise.pure(false);
                    }
                    ObjectNode oldProps = Json.newObject();
                    oldProps.put("name", name);
                    newProps.retain("uuid", "name", "description");
                    return Rule.nodes.update(oldProps, newProps);
                }
            });
        return updated.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        result.put("id", newProps.get("name").asText());
                        result.put("message", "Name successfully updated.");
                        return ok(result);
                    }
                    result.put("message", "Name not updated.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateDescription(String name) {
        ObjectNode newProps = (ObjectNode) request().body().asJson();
        newProps.retain("uuid", "name", "description");
        ObjectNode oldProps = newProps.deepCopy().retain("name");
        Promise<Boolean> updated = Rule.nodes.update(oldProps, newProps);
        return updated.map(
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
        final ObjectNode result = Json.newObject();
        final CombinationGroup group = CombinationGroup.of(groupID);
        JsonNode json = request().body().asJson();
        final String content = json.findPath("content").textValue();
        final OutputString string = OutputString.of(content);
        Promise<UUID> uuid = string.getUUID();
        Promise<Boolean> added = uuid.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID uuid) {
                    result.put("id", uuid.toString());
                    string.jsonProperties.put("uuid", uuid.toString());
                    return group.addString(string);
                }
            });
        return added.map(new ResultFunction("String successfully added.",
                                            "String not added", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateString(
        String name, String groupID, String stringID) {
        final ObjectNode result = Json.newObject();
        OutputString oldString = OutputString.of(UUID.fromString(stringID));
        final CombinationGroup group = CombinationGroup.of(groupID);
        Promise<Boolean> removed = group.removeString(oldString);
        Promise<Boolean> added = removed.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean removed) {
                    if (removed) {
                        JsonNode json = request().body().asJson();
                        final String content = json
                            .findPath("content").textValue();
                        final OutputString newString =
                            OutputString.of(content);
                        Promise<UUID> uuid = newString.getUUID();
                        Promise<Boolean> added = uuid.flatMap(
                            new Function<UUID, Promise<Boolean>>() {
                                public Promise<Boolean> apply(UUID uuid) {
                                    result.put("id", uuid.toString());
                                    newString.jsonProperties
                                        .put("uuid", uuid.toString());
                                    return group.addString(newString);
                                }
                            });
                        return added;
                    }
                    return Promise.pure(false);
                }
            });
        return added.map(new ResultFunction("String successfully updated.",
                                            "String not updated.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeString(
        String name, String groupID, String stringID) {
        OutputString string = OutputString.of(UUID.fromString(stringID));
        Promise<Boolean> removed = CombinationGroup.of(groupID)
            .removeString(string);
        return removed.map(new ResultFunction("String successfully removed.",
                                              "String not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addGroup(String name) {
        final ObjectNode result = Json.newObject();
        JsonNode json = request().body().asJson();
        final UUID uuid = UUID.randomUUID();
        result.put("id", uuid.toString());
        int position = json.findPath("position").intValue();
        CombinationGroup group = new CombinationGroup(uuid, position);
        Promise<Boolean> added = new Rule(name).addGroup(group);
        return added.map(new ResultFunction("Group successfully added.",
                                            "Group not added.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateGroup(String name, String groupID) {
        CombinationGroup group = CombinationGroup.of(groupID);
        JsonNode json = request().body().asJson();
        int position = json.findPath("position").intValue();
        Promise<Boolean> updated = group.update(position);
        return updated.map(new ResultFunction("Group successfully updated.",
                                              "Group not updated."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeGroup(String name, String groupID) {
        CombinationGroup group = CombinationGroup.of(groupID);
        Promise<Boolean> removed = new Rule(name).removeGroup(group);
        return removed.map(new ResultFunction("Group successfully removed.",
                                              "Group not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addSlot(String name, String groupID) {
        ObjectNode result = Json.newObject();
        JsonNode json = request().body().asJson();
        int position = json.findPath("position").intValue();
        UUID uuid = UUID.randomUUID();
        result.put("id", uuid.toString());
        Slot slot = Slot.of(uuid, position);
        Promise<Boolean> added = CombinationGroup.of(groupID).addSlot(slot);
        return added.map(new ResultFunction("Slot successfully added.",
                                            "Slot not added", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeSlot(
        String name, String groupID, String slotID) {
        Slot slot = Slot.of(UUID.fromString(slotID));
        Promise<Boolean> removed = CombinationGroup.of(groupID)
            .removeSlot(slot);
        return removed.map(new ResultFunction("Slot successfully removed.",
                                              "Slot not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addPart(
        String name, String groupID, String slotID) {
        final ObjectNode result = Json.newObject();
        final Slot slot = Slot.of(UUID.fromString(slotID));
        JsonNode json = request().body().asJson();
        final String content = json.findPath("content").textValue();
        final Part part = Part.of(content);
        Promise<UUID> uuid = part.getUUID();
        Promise<Boolean> added = uuid.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID uuid) {
                    result.put("id", uuid.toString());
                    part.jsonProperties.put("uuid", uuid.toString());
                    return slot.addPart(part);
                }
            });
        return added.map(new ResultFunction("Part successfully added.",
                                            "Part not added.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updatePart(
        String name, String groupID, String slotID, String partID) {
        final ObjectNode result = Json.newObject();
        Part oldPart = Part.of(UUID.fromString(partID));
        final Slot slot = Slot.of(UUID.fromString(slotID));
        Promise<Boolean> removed = slot.removePart(oldPart);
        Promise<Boolean> added = removed.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean removed) {
                    if (removed) {
                        JsonNode json = request().body().asJson();
                        final String content = json
                            .findPath("content").textValue();
                        final Part newPart = Part.of(content);
                        Promise<UUID> uuid = newPart.getUUID();
                        Promise<Boolean> added = uuid.flatMap(
                            new Function<UUID, Promise<Boolean>>() {
                                public Promise<Boolean> apply(UUID uuid) {
                                    result.put("id", uuid.toString());
                                    newPart.jsonProperties
                                        .put("uuid", uuid.toString());
                                    return slot.addPart(newPart);
                                }
                            });
                        return added;
                    }
                    return Promise.pure(false);
                }
            });
        return added.map(new ResultFunction("Part successfully updated.",
                                            "Part not updated.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removePart(
        String name, String groupID, String slotID, String partID) {
        Part part = Part.of(UUID.fromString(partID));
        Promise<Boolean> removed = Slot.of(UUID.fromString(slotID))
            .removePart(part);
        return removed.map(new ResultFunction("Part successfully removed.",
                                              "Part not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addRef(
        String name, String groupID, String slotID) {
        JsonNode json = request().body().asJson();
        String ruleName = json.findPath("ruleName").textValue();
        Promise<Boolean> added;
        if (name.equals(ruleName)) {
            added = Promise.pure(false);
            return added.map(new ResultFunction(
                                 "Cross-reference successfully added.",
                                 "Can't add circular dependency."));
        } else {
            ObjectNode result = Json.newObject();
            result.put("id", ruleName);
            final Slot slot = Slot.of(UUID.fromString(slotID));
            added = slot.addRef(new Rule(ruleName));
            return added.map(new ResultFunction(
                                 "Cross-reference successfully added.",
                                 "Cross-reference not added.", result));
        }
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeRef(
        String name, String groupID, String slotID, String refID) {
        Rule rule = new Rule(refID);
        Promise<Boolean> removed = Slot.of(UUID.fromString(slotID))
            .removeRef(rule);
        return removed.map(new ResultFunction("Part successfully removed.",
                                              "Part not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> delete(String name) {
        Promise<Boolean> deleted = new Rule(name).deleteIfOrphaned();
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

    private static class ResultFunction implements Function<Boolean, Result> {
        private String successMsg;
        private String errorMsg;
        private ObjectNode result;
        public ResultFunction(String successMsg, String errorMsg) {
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }
        public ResultFunction(String successMsg,
                              String errorMsg,
                              ObjectNode result) {
            this(successMsg, errorMsg);
            this.result = result;
        }
        public Result apply(Boolean actionSuccessful) {
            ObjectNode result =
                (this.result == null) ? Json.newObject() : this.result;
            if (actionSuccessful) {
                result.put("message", successMsg);
                return ok(result);
            }
            result.put("message", errorMsg);
            return badRequest(result);
        }
    }

}
