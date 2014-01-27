package controllers;

import java.util.List;

import play.Routes;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.None;
import play.libs.F.Tuple;

import play.data.validation.Constraints.Required;

import constants.FeatureType;
import models.AtomicFeature;
import models.ComplexFeature;
import models.Feature;
import models.AllowsRelationship;
import models.OntologyNode;
import models.Relationship;
import models.Value;

import views.html.features;

import static play.data.Form.form;


public class Features extends Controller {

    @Security.Authenticated(Secured.class)
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(Routes.javascriptRouter(
                      "jsRoutes",
                      controllers.routes.javascript.Features
                      .updateFeatureName(),
                      controllers.routes.javascript.Features
                      .updateFeatureDescription(),
                      controllers.routes.javascript.Features
                      .updateFeatureType(),
                      controllers.routes.javascript.Features
                      .deleteFeature(),
                      controllers.routes.javascript.Features
                      .addTargets(),
                      controllers.routes.javascript.Features
                      .deleteTarget()));
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
                    List<Feature> globalFeatureList = lists._1;
                    for (Feature feature: globalFeatureList) {
                        feature.setTargets();
                    }
                    List<Value> globalValueList = lists._2;
                    return ok(features.render(globalFeatureList,
                                              form(NewFeatureForm.class),
                                              globalValueList));
            }});
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> createFeature() {
        final Form<NewFeatureForm> featureForm =
            form(NewFeatureForm.class).bindFromRequest();
        // ...
        String name = featureForm.get().name;
        String description = featureForm.get().description;
        String type = featureForm.get().type;
        Promise<Tuple<Option<OntologyNode>, Boolean>> result = null;
        if (type.equals(FeatureType.COMPLEX.toString())) {
            result = new ComplexFeature(name, description).getOrCreate();
        } else if (type.equals(FeatureType.ATOMIC.toString())) {
            result = new AtomicFeature(name, description).getOrCreate();
        }
        return result.map(
            new Function<Tuple<Option<OntologyNode>, Boolean>, Result>() {
                public Result apply(
                    Tuple<Option<OntologyNode>, Boolean> result) {
                    Boolean created = result._2;
                    if (created) {
                        flash("success", "Feature successfully created.");
                    } else {
                        Option<OntologyNode> feature = result._1;
                        if (feature.isDefined()) {
                            flash("error", "Feature already exists.");
                        } else {
                            flash("error", "Feature creation failed.");
                        }
                    }
                    return redirect(routes.Features.list());
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> updateFeatureName(
        final String featureName) {
        DynamicForm nameForm = form().bindFromRequest();
        final String newName = nameForm.get("name");
        Promise<List<Feature>> features = Feature.all();
        Promise<Boolean> nameAlreadyTaken = features.map(
            new Function<List<Feature>, Boolean>() {
                public Boolean apply(List<Feature> features) {
                    Boolean nameAlreadyTaken = false;
                    for (Feature feature: features) {
                        if (feature.name.equals(newName)) {
                            nameAlreadyTaken = true;
                            break;
                        }
                    }
                    return nameAlreadyTaken;
                }
            });
        return nameAlreadyTaken.flatMap(
            new Function<Boolean, Promise<Result>>() {
                public Promise<Result> apply(Boolean nameAlreadyTaken) {
                    if (nameAlreadyTaken) {
                        return Promise.promise(
                            new Function0<Result>() {
                                public Result apply() {
                                    return badRequest();
                                }
                            });
                    } else {
                        Promise<Boolean> nameUpdated = new Feature(
                            featureName).updateName(newName);
                        return nameUpdated.map(
                            new Function<Boolean, Result>() {
                                public Result apply(Boolean updated) {
                                    if (updated) {
                                        return ok();
                                    }
                                    return badRequest();
                                }
                            });
                    }
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> updateFeatureDescription(
        String featureName) {
        DynamicForm descriptionForm = form().bindFromRequest();
        String newDescription = descriptionForm.get("description");
        Promise<Boolean> descriptionUpdated = new Feature(featureName)
            .updateDescription(newDescription);
        return descriptionUpdated.map(new Function<Boolean, Result>() {
                public Result apply(Boolean updated) {
                    if (updated) {
                        return ok();
                    }
                    return badRequest();
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> updateFeatureType(String featureName) {
        Form<UpdateFeatureTypeForm> typeForm =
            form(UpdateFeatureTypeForm.class).bindFromRequest();
        final String newType = typeForm.get().type;
        Promise<Tuple<Option<Feature>, Boolean>> result =
            new Feature(featureName).updateType(newType);
        return result.map(
            new Function<Tuple<Option<Feature>, Boolean>, Result>() {
                public Result apply(Tuple<Option<Feature>, Boolean> result) {
                    Boolean updated = result._2;
                    if (updated) {
                        if (newType.equals(FeatureType.COMPLEX.toString())) {
                            Value.deleteOrphans();
                        }
                        return ok();
                    }
                    return badRequest();
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> addTargets(String featureName) {
        Form<AddTargetForm> targetForm = form(AddTargetForm.class)
            .bindFromRequest();
        String featureType = targetForm.get().type;
        String targetName = targetForm.get().target;
        Promise<Tuple<Option<Relationship>, Boolean>> relationshipResult =
            null;
        if (featureType.equals(FeatureType.COMPLEX.toString())) {
            Feature feature = new ComplexFeature(featureName);
            Feature target = new Feature(targetName);
            relationshipResult = new AllowsRelationship(feature, target)
                .getOrCreate();
        } else if (featureType.equals(FeatureType.ATOMIC.toString())) {
            final Feature feature = new AtomicFeature(featureName);
            Promise<Tuple<Option<OntologyNode>, Boolean>> valueResult =
                new Value(targetName).getOrCreate();
            relationshipResult = valueResult.flatMap(
                new MaybeConnectToValueFunction(feature));
        }
        return relationshipResult.map(
            new Function<Tuple<Option<Relationship>, Boolean>, Result>() {
                public Result apply(
                    Tuple<Option<Relationship>, Boolean> relationshipResult) {
                    Boolean created = relationshipResult._2;
                    if (created) {
                        return ok();
                    }
                    return badRequest();
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> deleteTarget(String fname, String tname) {
        Form<DeleteTargetForm> targetForm = form(DeleteTargetForm.class)
            .bindFromRequest();
        Feature feature;
        final OntologyNode target;
        String featureType = targetForm.get().type;
        if (featureType.equals(FeatureType.COMPLEX.toString())) {
            feature = new ComplexFeature(fname);
            target = new Feature(tname);
        } else {
            feature = new AtomicFeature(fname);
            target = new Value(tname);
        }
        Promise<Boolean> deleted = new AllowsRelationship(
            feature, target).delete();
        if (target.isValue()) {
            deleted.onRedeem(
                new Callback<Boolean>() {
                    public void invoke(Boolean deleted) {
                        if (deleted) {
                            target.delete();
                        }
                    }
                });
        }
        return deleted.map(new Function<Boolean, Result>() {
                public Result apply(Boolean deleted) {
                    if (deleted) {
                        return ok();
                    }
                    return badRequest();
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> deleteFeature(final String featureName) {
        Promise<Boolean> isInUse = new Feature(featureName).isInUse();
        Promise<Boolean> deleted = isInUse.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean isInUse) {
                    if (isInUse) {
                        return Promise.pure(false);
                    }
                    return new Feature(featureName).delete();
                }
            });
        return deleted.map(
            new Function<Boolean, Result>() {
                public Result apply(Boolean deleted) {
                    if (deleted) {
                        Value.deleteOrphans();
                        return ok();
                    }
                    return badRequest();
                }
            });
    }


    private static class MaybeConnectToValueFunction
        implements Function<Tuple<Option<OntologyNode>, Boolean>,
                            Promise<Tuple<Option<Relationship>, Boolean>>> {
        private Feature feature;
        public MaybeConnectToValueFunction(Feature feature) {
            this.feature = feature;
        }
        public Promise<Tuple<Option<Relationship>, Boolean>> apply(
            Tuple<Option<OntologyNode>, Boolean> valueResult) {
            Option<OntologyNode> value = valueResult._1;
            if (value.isDefined()) {
                return new AllowsRelationship(
                    feature, value.get()).getOrCreate();
            }
            return Promise.pure(
                new Tuple<Option<Relationship>, Boolean>(
                    new None<Relationship>(), false));
        }
    }


    public static class NewFeatureForm {
        @Required
        public String name;
        @Required
        public String type;
        public String description;
    }

    public static class UpdateFeatureTypeForm {
        public String type;
    }

    public static class AddTargetForm {
        public String type;
        public String target;
    }

    public static class DeleteTargetForm extends UpdateFeatureTypeForm {}

}
