package controllers;

import play.data.*;
import java.util.List;

import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import models.Feature;
import models.User;
import models.Value;
import views.html.*;

import static play.data.Form.*;


public class Application extends Controller {

    public static Result home() {
        return ok(home.render(form(Registration.class)));
    }

    public static Promise<Result> register() {
        final Form<Registration> registrationForm =
            form(Registration.class).bindFromRequest();
        if (registrationForm.hasErrors()) {
            Promise<Result> errorResult = Promise.promise(
                new Function0<Result>() {
                    public Result apply() {
                        return badRequest(home.render(registrationForm));
                }
            });
            return errorResult;
        }
        final User user = new User(registrationForm.get().email,
                                   registrationForm.get().password);
        if (user.exists().get()) {
            return Promise.promise(new Function0<Result>() {
                public Result apply() {
                    flash("error", "User already exists.");
                    return redirect(routes.Application.home());
                }});
        } else {
            Promise<User> newUser = user.create();
            return newUser.map(new Function<User, Result>() {
                public Result apply(User user) {
                    if (user == null) {
                        flash("error", "Registration failed.");
                    } else {
                        flash("success", "Registration successful.");
                    }
                    return redirect(routes.Application.home());
                }
            });
        }
    }

    public static Result login() {
        return ok(login.render(form(Login.class)));
    }

    public static Promise<Result> authenticate() {
        final Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            Promise<Result> errorResult = Promise.promise(
                new Function0<Result>() {
                    public Result apply() {
                        return badRequest(login.render(loginForm));
                    }
                }
            );
            return errorResult;
        }
        Promise<User> user = new User(loginForm.get().email,
                                      loginForm.get().password).get();
        return user.map(new Function<User, Result>() {
            public Result apply(User user) {
                if (user == null) {
                    flash("error", "Login failed: Unknown user.");
                    return badRequest(login.render(loginForm));
                } else {
                    session().clear();
                    session("email", loginForm.get().email);
                    flash("success", "Login successful.");
                    return redirect(routes.Application.home());
                }
            }
        });
    }

    @Security.Authenticated(Secured.class)
    public static Result logout() {
        session().clear();
        flash("success", "Alright. See you around.");
        return redirect(routes.Application.login());
    }

    @Security.Authenticated(Secured.class)
    public static Result rules() {
        return ok(rules.render("Hi! This is Ode's Rule Browser."));
    }

    @Security.Authenticated(Secured.class)
    public static Result rule(int id) {
        return ok(rule.render("Hi! You are looking at rule " + id + "."));
    }

    @Security.Authenticated(Secured.class)
    public static Result search() {
        return ok(search.render("Hi! This is Ode's Search Interface."));
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> features() {
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
    public static Promise<Result> feature() {
        final Form<NewFeatureForm> featureForm =
            form(NewFeatureForm.class).bindFromRequest();
        final Feature feature = new Feature(featureForm.get().name,
                                            featureForm.get().type);
        if (feature.exists().get()) {
            return Promise.promise(new Function0<Result>() {
                public Result apply() {
                    flash("error", "Feature already exists.");
                    return redirect(routes.Application.features());
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
                    return redirect(routes.Application.features());
                }});
        }
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> updateFeature(String name) {
        final Form<UpdateFeatureForm> featureForm =
            form(UpdateFeatureForm.class).bindFromRequest();
        final Feature feature = new Feature(name, featureForm.get().type);
        Promise<Feature> updatedFeature = feature.update();
        return updatedFeature.map(new Function<Feature, Result>() {
            public Result apply(Feature feature) {
                if (feature.featureType.equals("complex")) {
                    Feature permittedFeature = new Feature(
                        featureForm.get().feature);
                    feature.connectTo(permittedFeature, "ALLOWS");
                } else if (feature.featureType.equals("atomic")) {
                    String valueName = featureForm.get().value;
                    Value permittedValue = new Value(valueName);
                    if (!permittedValue.exists().get()) {
                        permittedValue.create();
                    }
                    feature.connectTo(permittedValue, "ALLOWS");
                }
                return redirect(routes.Application.features());
            }});
    }

    // Forms

    public static class Login {
        public String email;
        public String password;

        public String validate() {
            if (email.isEmpty() || password.isEmpty()) {
                return "You must provide input for all fields.";
            }
            return null;
        }
    }

    public static class Registration extends Login {}

    public static class NewFeatureForm {
        public String name;
        public String type;
    }

    public static class UpdateFeatureForm {
        public String type;
        public String feature;
        public String value;
    }

}
