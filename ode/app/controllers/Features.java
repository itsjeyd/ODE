package controllers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Routes;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import play.libs.Json;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import constants.FeatureType;
import models.nodes.Feature;
import models.nodes.Value;

import views.html.features;


public class Features extends Controller {

    private enum TargetAction {
        ADD, REMOVE;
    }

    @Security.Authenticated(Secured.class)
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(Routes.javascriptRouter(
                      "jsFeatureRoutes",
                      controllers.routes.javascript.Features
                      .create(),
                      controllers.routes.javascript.Features
                      .updateName(),
                      controllers.routes.javascript.Features
                      .updateDescription(),
                      controllers.routes.javascript.Features
                      .updateType(),
                      controllers.routes.javascript.Features
                      .updateTargets(),
                      controllers.routes.javascript.Features
                      .delete()));
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> features() {
        Promise<List<Feature>> globalFeatureList = Feature.nodes.all();
        Promise<List<Value>> globalValueList = Value.nodes.all();
        Promise<Tuple<List<Feature>, List<Value>>> lists = globalFeatureList
            .zip(globalValueList);
        return lists.map(
            new Function<Tuple<List<Feature>, List<Value>>, Result>() {
                public Result apply(
                    Tuple<List<Feature>, List<Value>> lists) {
                    return ok(features.render(lists._1, lists._2));
            }});
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> create() {
        final JsonNode json = request().body().asJson();
        ObjectNode props = (ObjectNode) json.deepCopy();
        props.remove("targets");
        Promise<Boolean> created = Feature.nodes.create(props);
        return created.map(
            new Function<Boolean, Result>() {
                ObjectNode jsonResult = Json.newObject();
                public Result apply(Boolean created) {
                    if (created) {
                        String name = json.get("name").asText();
                        jsonResult.put("id", name);
                        jsonResult.put("message",
                                       "Feature successfully created.");
                        return ok(jsonResult);
                    }
                    jsonResult.put("error", "Feature not created.");
                    return badRequest(jsonResult);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateName(final String name) {
        final ObjectNode newProps = (ObjectNode) request().body().asJson();
        Promise<Boolean> nameTaken =
            Feature.nodes.exists(newProps.deepCopy().retain("name"));
        Promise<Boolean> updated = nameTaken.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean nameTaken) {
                    if (nameTaken) {
                        return Promise.pure(false);
                    }
                    ObjectNode oldProps = Json.newObject();
                    oldProps.put("name", name);
                    newProps.retain("name", "description", "type");
                    return Feature.nodes.update(oldProps, newProps);
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
        newProps.retain("name", "description", "type");
        ObjectNode oldProps = newProps.deepCopy().retain("name");
        Promise<Boolean> updated = Feature.nodes.update(oldProps, newProps);
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
    public static Promise<Result> updateType(String name) {
        JsonNode json = request().body().asJson();
        final ObjectNode props = (ObjectNode) json.deepCopy();
        // 1. Check if feature is orphaned
        Promise<Boolean> orphaned = Feature.nodes
            .orphaned(props.deepCopy().retain("name"));
        // 2. If it is, update its type
        Promise<Boolean> updated = orphaned.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean orphaned) {
                    if (orphaned) {
                        return Feature.nodes.setType(props);
                    }
                    return Promise.pure(false);
                }
            });
        // 3. If previous type == "atomic", delete orphans
        if (props.get("type").asText().equals("complex")) {
            updated.onRedeem(
                new Callback<Boolean>() {
                    public void invoke(Boolean updated) {
                        if (updated) {
                            Value.nodes.delete();
                        }
                    }
                });
        }
        return updated.map(
            new Function<Boolean, Result>() {
                ObjectNode jsonResult = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        jsonResult.put("message",
                                       "Type successfully updated.");
                        return ok(jsonResult);
                    }
                    jsonResult.put("message", "Type not updated.");
                    return badRequest(jsonResult);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateTargets(String name) {
        JsonNode json = request().body().asJson();
        TargetAction action = TargetAction.valueOf(
            json.findPath("action").textValue());
        switch (action) {
            case ADD: return addTarget(name, json);
            case REMOVE: return removeTarget(name, json);
            default: return Promise.promise(
                new Function0<Result>() {
                    ObjectNode result = Json.newObject();
                    public Result apply() {
                        result.put(
                            "message", "Unknown action.");
                        return badRequest(result);
                    }
                });
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    private static Promise<Result> removeTarget(String name, JsonNode json) {
        final ObjectNode feature = Json.newObject();
        feature.put("name", name);
        feature.put("type", json.get("type").asText());
        final ObjectNode target = Json.newObject();
        target.put("name", json.get("target").asText());
        Promise<Boolean> hasValue = Feature.nodes.has(feature, target);
        Promise<Boolean> disconnected = hasValue.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean hasValue) {
                    if (hasValue) {
                        return Promise.pure(false);
                    }
                    return Feature.nodes.disconnect(feature, target);
                }
            });
        if (feature.get("type").asText().equals("atomic")) {
            disconnected.onRedeem(
                new Callback<Boolean>() {
                    public void invoke(Boolean disconnected) {
                        if (disconnected) {
                            Promise<Boolean> orphaned =
                                Value.nodes.orphaned(target);
                            orphaned.onRedeem(
                                new Callback<Boolean>() {
                                    public void invoke(Boolean orphaned) {
                                        if (orphaned) {
                                            Value.nodes.delete(target);
                                        }
                                    }
                                });
                        }
                    }
                });
        }
        return disconnected.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean disconnected) {
                    if (disconnected) {
                        result.put("message", "Target successfully removed.");
                        return ok(result);
                    }
                    result.put("message", "Target not removed.");
                    return badRequest(result);
                }
            });
    }

    @BodyParser.Of(BodyParser.Json.class)
    private static Promise<Result> addTarget(String name, JsonNode json) {
        ObjectNode feature = Json.newObject();
        feature.put("name", name);
        feature.put("type", json.get("type").asText());
        ObjectNode target = Json.newObject();
        target.put("name", json.get("target").asText());
        Promise<Boolean> connected = Feature.nodes.connect(feature, target);
        return connected.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean connected) {
                    if (connected) {
                        result.put("message", "Target successfully added.");
                        return ok(result);
                    }
                    result.put("message", "Target not added.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> delete(final String name) {
        final ObjectNode props = Json.newObject();
        props.put("name", name);
        // 1. Check if feature is orphaned
        Promise<Boolean> orphaned = Feature.nodes.orphaned(props);
        Promise<Boolean> deleted = orphaned.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean orphaned) {
                    // 2. If it isn't, delete it
                    if (orphaned) {
                        Promise<Feature> feature = Feature.nodes.get(props);
                        Promise<Boolean> deleted = feature.flatMap(
                            new Function<Feature, Promise<Boolean>>() {
                                public Promise<Boolean> apply(
                                    Feature feature) {
                                    Promise<Boolean> deleted =
                                        Feature.nodes.delete(props);
                                    // 3. If type == "atomic", delete orphans
                                    if (feature.getType().equals(
                                            FeatureType.ATOMIC.toString())) {
                                        deleted.onRedeem(
                                            new Callback<Boolean>() {
                                                public void invoke(
                                                    Boolean deleted) {
                                                    if (deleted) {
                                                        Value.nodes.delete();
                                                    }
                                                }});
                                    }
                                    return deleted;
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
                        result.put("message",
                                   "Feature successfully deleted.");
                        return ok(result);
                    }
                    result.put("message", "Feature not deleted.");
                    return badRequest(result);
                }
            });
    }

}
