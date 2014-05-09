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
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import constants.FeatureType;
import models.nodes.AtomicFeature;
import models.nodes.ComplexFeature;
import models.nodes.Feature;
import models.nodes.OntologyNode;
import models.nodes.Value;
import models.relationships.AllowsRelationship;

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
                      .createFeature(),
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
    public static Promise<Result> list() {
        Promise<List<Feature>> globalFeatureList = Feature.all();
        Promise<List<Value>> globalValueList = Value.all();
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
    public static Promise<Result> createFeature() {
        JsonNode json = request().body().asJson();
        final String name = json.findPath("name").textValue();
        String description = json.findPath("description").textValue();
        String type = json.findPath("type").textValue();
        Promise<Boolean> created = null;
        if (type.equals(FeatureType.COMPLEX.toString())) {
            created = new ComplexFeature(name, description).create();
        } else if (type.equals(FeatureType.ATOMIC.toString())) {
            created = new AtomicFeature(name, description).create();
        }
        return created.map(
            new Function<Boolean, Result>() {
                ObjectNode jsonResult = Json.newObject();
                public Result apply(Boolean created) {
                    if (created) {
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
        JsonNode json = request().body().asJson();
        final String newName = json.findPath("name").textValue();
        Promise<Boolean> nameAlreadyTaken = new Feature(newName).exists();
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
                        Promise<Boolean> nameUpdated = new Feature(
                            name).updateName(newName);
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
        Promise<Boolean> descriptionUpdated = new Feature(name)
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
    public static Promise<Result> updateType(String name) {
        JsonNode json = request().body().asJson();
        final String newType = json.findPath("type").textValue();
        Promise<Tuple<Option<Feature>, Boolean>> result =
            new Feature(name).updateType(newType);
        return result.map(
            new Function<Tuple<Option<Feature>, Boolean>, Result>() {
                ObjectNode jsonResult = Json.newObject();
                public Result apply(Tuple<Option<Feature>, Boolean> result) {
                    Boolean updated = result._2;
                    if (updated) {
                        if (newType.equals(FeatureType.COMPLEX.toString())) {
                            Value.deleteOrphans();
                        }
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
        Feature feature;
        String featureType = json.findPath("type").textValue();
        final OntologyNode target;
        String targetName = json.findPath("target").textValue();
        if (featureType.equals(FeatureType.COMPLEX.toString())) {
            feature = new ComplexFeature(name);
            target = new Feature(targetName);
        } else {
            feature = new AtomicFeature(name);
            target = new Value(targetName);
        }
        final Feature allowingFeature = feature;
        Promise<Boolean> hasTarget = feature.has(target);
        Promise<Boolean> deleted = hasTarget.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean hasTarget) {
                    if (hasTarget) {
                        return Promise.pure(false);
                    }
                    return new AllowsRelationship(allowingFeature, target)
                        .delete();
                }
            });
        if (target.isValue()) {
            final Value value = (Value) target;
            deleted.onRedeem(
                new Callback<Boolean>() {
                    public void invoke(Boolean deleted) {
                        if (deleted && !value.name.equals("underspecified")) {
                            value.deleteIfOrphaned();
                        }
                    }
                });
        }
        return deleted.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean deleted) {
                    if (deleted) {
                        result.put("message",
                                   "Target successfully removed.");
                        return ok(result);
                    }
                    result.put("message", "Target not removed.");
                    return badRequest(result);
                }
            });
    }

    @BodyParser.Of(BodyParser.Json.class)
    private static Promise<Result> addTarget(String name, JsonNode json) {
        String featureType = json.findPath("type").textValue();
        String targetName = json.findPath("target").textValue();
        Promise<Boolean> connected = null;
        if (featureType.equals(FeatureType.COMPLEX.toString())) {
            Feature feature = new ComplexFeature(name);
            Feature target = new Feature(targetName);
            connected = target.connectTo(feature);
        } else if (featureType.equals(FeatureType.ATOMIC.toString())) {
            Feature feature = new AtomicFeature(name);
            Value target = new Value(targetName);
            connected = target.connectTo(feature);
        }
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
        Promise<Boolean> isInUse = new Feature(name).isInUse();
        Promise<Boolean> deleted = isInUse.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean isInUse) {
                    if (isInUse) {
                        return Promise.pure(false);
                    }
                    return new Feature(name).delete();
                }
            });
        return deleted.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean deleted) {
                    if (deleted) {
                        Value.deleteOrphans();
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
