package controllers;

import java.util.List;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import play.data.validation.Constraints.Required;

import models.Feature;
import models.Model;
import models.Relationship;
import models.Value;

import views.html.features;

import static play.data.Form.form;


public class Features extends Controller {

    @Security.Authenticated(Secured.class)
    public static Promise<Result> list() {
        Promise<List<Feature>> featureList = Feature.all();
        Promise<List<Value>> globalValueList = Value.all();
        Promise<Tuple<List<Feature>, List<Value>>> lists = featureList
            .zip(globalValueList);
        return lists.map(
            new Function<Tuple<List<Feature>, List<Value>>, Result>() {
                public Result apply(
                    Tuple<List<Feature>, List<Value>> lists) {
                    List<Feature> featureList = lists._1;
                    for (Feature feat: featureList) {
                        feat.values = feat.getValues().get();
                    }
                    List<Value> globalValueList = lists._2;
                    return ok(features.render(featureList,
                                              form(NewFeatureForm.class),
                                              globalValueList));
            }});
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> createFeature() {
        final Form<NewFeatureForm> featureForm =
            form(NewFeatureForm.class).bindFromRequest();
        final Feature feature = new Feature(featureForm.get().name,
                                            featureForm.get().type,
                                            featureForm.get().description);
        if (feature.exists().get()) {
            return Promise.promise(new Function0<Result>() {
                public Result apply() {
                    flash("error", "Feature already exists.");
                    return redirect(routes.Features.list());
                }});
        } else {
            Promise<Feature> newFeature = feature.create();
            return newFeature.map(new Function<Feature, Result>() {
                public Result apply(Feature feature) {
                    if (feature == null) {
                        flash("error", "Feature creation failed.");
                    } else {
                        flash("success", "Feature successfully created.");
                    }
                    return redirect(routes.Features.list());
                }});
        }
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> updateFeatureType(String featureName) {
        Form<UpdateFeatureTypeForm> typeForm =
            form(UpdateFeatureTypeForm.class).bindFromRequest();
        Feature featureToUpdate = new Feature(
            featureName, typeForm.get().type);
        Promise<Feature> updatedFeature = featureToUpdate.update();
        return updatedFeature.map(new Function<Feature, Result>() {
            public Result apply(Feature updatedFeature) {
                if (updatedFeature == null) {
                    flash("error", "Feature type not updated.");
                } else {
                    flash("success", "Feature type successfully updated.");
                }
                return redirect(routes.Features.list());
            }
        });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> addTargets(String featureName) {
        Form<AddTargetForm> targetForm = form(AddTargetForm.class)
            .bindFromRequest();
        Feature featureToAddTargetsTo = new Feature(
            featureName, targetForm.get().type);
        Model target = null;
        if (featureToAddTargetsTo.featureType.equals("complex")) {
            target = new Feature(targetForm.get().target);
        } else {
            target = new Value(targetForm.get().target);
            if (!target.exists().get()) {
                target.create();
            }
        }
        Promise<Relationship> allowsRelationship = featureToAddTargetsTo
            .connectTo(target, "ALLOWS");
        return allowsRelationship.map(new Function<Relationship, Result>() {
            public Result apply(Relationship relationship) {
                if (relationship == null) {
                    flash("error", "Operation failed.");
                } else {
                    flash("success", "Operation successfully completed.");
                }
                return redirect(routes.Features.list());
            }
        });
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
