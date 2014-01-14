package controllers;

import java.util.List;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import play.libs.F.Function;
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
    public static Promise<Result> updateFeatureType(String featureName) {
        Form<UpdateFeatureTypeForm> typeForm =
            form(UpdateFeatureTypeForm.class).bindFromRequest();
        String newType = typeForm.get().type;
        Promise<Tuple<Option<Feature>, Boolean>> result =
            new Feature(featureName).updateType(newType);
        return result.map(
            new Function<Tuple<Option<Feature>, Boolean>, Result>() {
                public Result apply(Tuple<Option<Feature>, Boolean> result) {
                    Boolean updated = result._2;
                    if (updated) {
                        flash(
                            "success", "Feature type successfully updated.");
                    } else {
                        Option<Feature> feature = result._1;
                        if (feature.isDefined()) {
                            flash(
                                "error", "Same type. No updates performed.");
                        } else {
                            flash("error", "Feature type not updated.");
                        }
                    }
                    return redirect(routes.Features.list());
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
                        flash("success", "Operation successfully completed.");
                    } else {
                        Option<Relationship> relationship =
                            relationshipResult._1;
                        if (relationship.isDefined()) {
                            flash("error", "Can't add target twice.");
                        } else {
                            flash("error", "Operation failed.");
                        }
                    }
                    return redirect(routes.Features.list());
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
}
