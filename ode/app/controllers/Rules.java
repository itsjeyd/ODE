package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.List;
import java.util.UUID;
import models.nodes.CombinationGroup;
import models.nodes.Feature;
import models.nodes.LHS;
import models.nodes.Part;
import models.nodes.RHS;
import models.nodes.Rule;
import models.nodes.Slot;
import models.nodes.Substructure;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.UUIDGenerator;
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
        ObjectNode properties = Json.newObject();
        properties.put("name", name);
        Promise<Rule> requestedRule = Rule.nodes.full(properties);
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
        ObjectNode properties = Json.newObject();
        properties.put("name", name);
        Promise<Rule> rule = Rule.nodes.get(properties);
        rule = rule.flatMap(
            new Function<Rule, Promise<Rule>>() {
                public Promise<Rule> apply(final Rule rule) {
                    ObjectNode properties = Json.newObject();
                    String uuid = UUIDGenerator.from(rule.uuid);
                    properties.put("uuid", uuid);
                    properties.put("ruleUUID", rule.uuid);
                    Promise<LHS> lhs = LHS.nodes.get(properties);
                    return lhs.map(
                        new Function<LHS, Rule>() {
                            public Rule apply(LHS lhs) {
                                rule.lhs = lhs;
                                return rule;
                            }
                        });
                }
            });
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
        ObjectNode properties = Json.newObject();
        properties.put("name", name);
        Promise<Rule> rule = Rule.nodes.get(properties);
        rule = rule.flatMap(
            new Function<Rule, Promise<Rule>>() {
                public Promise<Rule> apply(final Rule rule) {
                    ObjectNode properties = Json.newObject();
                    String uuid = UUIDGenerator.from(rule.uuid);
                    properties.put("uuid", uuid);
                    Promise<RHS> rhs = RHS.nodes.get(properties);
                    properties.put("ruleUUID", rule.uuid);
                    Promise<LHS> lhs = LHS.nodes.get(properties);
                    return lhs.zip(rhs).map(
                        new Function<Tuple<LHS, RHS>, Rule>() {
                            public Rule apply(Tuple<LHS, RHS> components) {
                                rule.lhs = components._1;
                                rule.rhs = components._2;
                                return rule;
                            }
                        });
                }
            });
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
        ObjectNode properties = Json.newObject();
        properties.put("name", name);
        Promise<Rule> rule = Rule.nodes.get(properties);
        Promise<JsonNode> lhsJSON = rule.flatMap(
            new Function<Rule, Promise<JsonNode>>() {
                public Promise<JsonNode> apply(Rule rule) {
                    ObjectNode properties = Json.newObject();
                    String uuid = UUIDGenerator.from(rule.uuid);
                    properties.put("uuid", uuid);
                    properties.put("ruleUUID", rule.uuid);
                    Promise<LHS> lhs = LHS.nodes.get(properties);
                    return lhs.map(
                        new Function<LHS, JsonNode>() {
                            public JsonNode apply(LHS lhs) {
                                return lhs.json;
                            }
                        });
                }
            });
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
        ObjectNode properties = Json.newObject();
        properties.put("name", name);
        Promise<Rule> rule = Rule.nodes.get(properties);
        Promise<JsonNode> rhsJSON = rule.flatMap(
            new Function<Rule, Promise<JsonNode>>() {
                public Promise<JsonNode> apply(Rule rule) {
                    ObjectNode properties = Json.newObject();
                    String uuid = UUIDGenerator.from(rule.uuid);
                    properties.put("uuid", uuid);
                    Promise<RHS> rhs = RHS.nodes.get(properties);
                    return rhs.map(
                        new Function<RHS, JsonNode>() {
                            public JsonNode apply(RHS rhs) {
                                return rhs.json;
                            }
                        });
                }
            });
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
        newProps.retain("uuid", "name", "description");
        Promise<Boolean> nameTaken = Rule.nodes.exists(newProps);
        Promise<Boolean> updated = nameTaken.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean nameTaken) {
                    if (nameTaken) {
                        return Promise.pure(false);
                    }
                    ObjectNode oldProps = Json.newObject();
                    oldProps.put("name", name);
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
        final ObjectNode avm = (ObjectNode) json.deepCopy();
        avm.retain("ruleUUID", "uuid");
        final ObjectNode feature = Json.newObject();
        feature.put("name", json.findValue("name").asText());
        feature.put("type", json.findValue("type").asText());
        Promise<Boolean> added = Substructure.nodes.connect(avm, feature);
        return added.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean added) {
                    if (added) {
                        String type = feature.get("type").asText();
                        if (type.equals("complex")) {
                            String parentUUID = avm.get("uuid").asText();
                            String fname = feature.get("name").asText();
                            String uuid = UUIDGenerator
                                .from(parentUUID + fname);
                            ObjectNode value = Json.newObject();
                            value.put("uuid", uuid);
                            value.putArray("pairs");
                            result.put("value", value);
                        } else {
                            result.put(
                                "value", new TextNode("underspecified"));
                        }
                        result.put("message", "Feature successfully added.");
                        return ok(result);
                    }
                    result.put("message", "Feature not added.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateFeatureValue(String name) {
        JsonNode json = request().body().asJson();
        ObjectNode avm = (ObjectNode) json.deepCopy();
        avm.retain("ruleUUID", "uuid");
        ObjectNode feature = Json.newObject();
        feature.put("name", json.findValue("name").asText());
        ObjectNode value = Json.newObject();
        value.put("name", json.findValue("value").asText());
        ObjectNode newValue = Json.newObject();
        newValue.put("name", json.findValue("newValue"));
        Promise<Boolean> updated = Substructure.nodes
            .setValue(avm, feature, value, newValue);
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
        ObjectNode avm = (ObjectNode) json.deepCopy();
        avm.retain("ruleUUID", "uuid");
        ObjectNode feature = Json.newObject();
        feature.put("name", json.findValue("name").asText());
        feature.put("type", json.findValue("type").asText());
        Promise<Boolean> removed = Substructure.nodes
            .disconnect(avm, feature);
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
        ObjectNode group = Json.newObject();
        group.put("uuid", groupID);
        ObjectNode string = Json.newObject();
        String uuid = UUIDGenerator.from(json.get("content").asText());
        string.put("uuid", uuid);
        string.put("content", json.get("content").asText());
        Promise<Boolean> connected = CombinationGroup.nodes
            .connect(group, string);
        ObjectNode result = Json.newObject();
        result.put("id", uuid);
        return connected.map(new ResultFunction(
                                 "String successfully added.",
                                 "String not added.", result));

    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateString(
        String name, String groupID, String stringID) {
        JsonNode json = request().body().asJson();
        final ObjectNode group = Json.newObject();
        group.put("uuid", groupID);
        final ObjectNode oldString = Json.newObject();
        oldString.put("uuid", stringID);
        ObjectNode newString = Json.newObject();
        String content = json.findValue("content").asText();
        String uuid = UUIDGenerator.from(content);
        newString.put("content", content);
        newString.put("uuid", uuid);
        final ObjectNode result = Json.newObject();
        result.put("id", uuid);
        Promise<Boolean> updated = CombinationGroup.nodes
            .update(group, oldString, newString);
        return updated.map(
            new ResultFunction("String successfully updated.",
                               "String not updated.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeString(
        String name, String groupID, String stringID) {
        ObjectNode group = Json.newObject();
        group.put("uuid", groupID);
        final ObjectNode string = Json.newObject();
        string.put("uuid", stringID);
        string.put("nodeType", "string");
        Promise<Boolean> removed = CombinationGroup.nodes
            .disconnect(group, string);
        return removed.map(new ResultFunction("String successfully removed.",
                                              "String not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addGroup(String name) {
        final JsonNode json = request().body().asJson();
        ObjectNode rhs = Json.newObject();
        rhs.put("uuid", json.findValue("rhsID").asText());
        final ObjectNode group = Json.newObject();
        final String uuid = UUIDGenerator.random();
        group.put("uuid", uuid);
        group.put("position", json.findValue("position").asInt());
        final ObjectNode result = Json.newObject();
        result.put("id", uuid);
        Promise<Boolean> added = RHS.nodes.connect(rhs, group);
        return added.map(new ResultFunction("Group successfully added.",
                                            "Group not added.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateGroup(String name, String groupID) {
        JsonNode json = request().body().asJson();
        ObjectNode oldProps = Json.newObject();
        oldProps.put("uuid", groupID);
        ObjectNode newProps = oldProps.deepCopy();
        newProps.put("position", json.findValue("position").asInt());
        Promise<Boolean> updated = CombinationGroup.nodes
            .update(oldProps, newProps);
        return updated.map(new ResultFunction("Group successfully updated.",
                                              "Group not updated."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeGroup(
        String name, final String groupID) {
        Promise<UUID> ruleUUID = new Rule(name).getUUID();
        Promise<Boolean> removed = ruleUUID.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID ruleUUID) {
                    String uuid = UUIDGenerator.from(ruleUUID.toString());
                    ObjectNode rhs = Json.newObject();
                    rhs.put("uuid", uuid);
                    ObjectNode group = Json.newObject();
                    group.put("uuid", groupID);
                    return RHS.nodes.disconnect(rhs, group);
                }
            });
        return removed.map(new ResultFunction("Group successfully removed.",
                                              "Group not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addSlot(String name, final String groupID) {
        JsonNode json = request().body().asJson();
        ObjectNode group = Json.newObject();
        group.put("uuid", groupID);
        final ObjectNode slot = Json.newObject();
        final String uuid = UUIDGenerator.random();
        slot.put("uuid", uuid);
        slot.put("position", json.findValue("position").asInt());
        final ObjectNode result = Json.newObject();
        result.put("id", uuid);
        Promise<Boolean> added = CombinationGroup.nodes.connect(group, slot);
        return added.map(new ResultFunction("Slot successfully added.",
                                            "Slot not added", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeSlot(
        String name, String groupID, String slotID) {
        ObjectNode group = Json.newObject();
        group.put("uuid", groupID);
        ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        slot.put("nodeType", "slot");
        Promise<Boolean> removed = CombinationGroup.nodes
            .disconnect(group, slot);
        return removed.map(new ResultFunction("Slot successfully removed.",
                                              "Slot not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addPart(
        String name, String groupID, String slotID) {
        JsonNode json = request().body().asJson();
        ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        ObjectNode part = Json.newObject();
        String content = json.findValue("content").asText();
        String uuid = UUIDGenerator.from(content);
        part.put("uuid", uuid);
        part.put("content", content);
        Promise<Boolean> connected = Slot.nodes.connect(slot, part);
        ObjectNode result = Json.newObject();
        result.put("id", uuid);
        return connected.map(new ResultFunction("Part successfully added.",
                                                "Part not added.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updatePart(
        String name, String groupID, String slotID, String partID) {
        JsonNode json = request().body().asJson();
        final ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        final ObjectNode oldPart = Json.newObject();
        oldPart.put("uuid", partID);
        ObjectNode newPart = Json.newObject();
        String content = json.findValue("content").asText();
        String uuid = UUIDGenerator.from(content);
        newPart.put("content", content);
        newPart.put("uuid", uuid);
        final ObjectNode result = Json.newObject();
        result.put("id", uuid);
        Promise<Boolean> updated = Slot.nodes
            .update(slot, oldPart, newPart);
        return updated.map(new ResultFunction("Part successfully updated.",
                                              "Part not updated.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removePart(
        String name, String groupID, String slotID, String partID) {
        ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        final ObjectNode part = Json.newObject();
        part.put("uuid", partID);
        Promise<Boolean> removed = Slot.nodes.disconnect(slot, part);
        return removed.map(new ResultFunction("Part successfully removed.",
                                              "Part not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addRef(
        String name, String groupID, String slotID) {
        JsonNode json = request().body().asJson();
        String ruleName = json.findValue("ruleName").asText();
        Promise<Boolean> added;
        if (name.equals(ruleName)) {
            added = Promise.pure(false);
            return added.map(new ResultFunction(
                                 "Cross-reference successfully added.",
                                 "Can't add circular dependency."));
        } else {
            ObjectNode result = Json.newObject();
            result.put("id", ruleName);
            ObjectNode slot = Json.newObject();
            slot.put("uuid", slotID);
            ObjectNode rule = Json.newObject();
            rule.put("name", ruleName);
            added = Slot.nodes.connect(slot, rule);
            return added.map(new ResultFunction(
                                 "Cross-reference successfully added.",
                                 "Cross-reference not added.", result));
        }
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeRef(
        String name, String groupID, String slotID, String refID) {
        ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        ObjectNode rule = Json.newObject();
        rule.put("name", refID);
        Promise<Boolean> removed = Slot.nodes.disconnect(slot, rule);
        return removed.map(new ResultFunction("Part successfully removed.",
                                              "Part not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> delete(final String name) {
        final ObjectNode rule = Json.newObject();
        rule.put("name", name);
        // 1. Check if rule is orphaned
        Promise<Boolean> orphaned = Rule.nodes.orphaned(rule);
        // 2. If it is, delete it
        Promise<Boolean> deleted = orphaned.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean orphaned) {
                    if (orphaned) {
                        Promise<UUID> uuid = new Rule(name).getUUID();
                        Promise<Boolean> deleted = uuid.flatMap(
                            new Function<UUID, Promise<Boolean>>() {
                                public Promise<Boolean> apply(UUID uuid) {
                                    rule.put("uuid", uuid.toString());
                                    return Rule.nodes.delete(rule);
                                }
                            });
                        return deleted;
                    }
                    return Promise.pure(false);
                }
            });
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
