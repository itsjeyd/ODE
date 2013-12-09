package controllers;

import play.data.*;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import views.html.*;

import models.Node;
import models.User;

import static play.data.Form.*;


public class Application extends Controller {

    public static Result home() {
        return ok(home.render("Hi! This is Ode.", form(Registration.class)));
    }

    public static Promise<Result> register() {
        final Form<Registration> registrationForm =
            form(Registration.class).bindFromRequest();
        if (registrationForm.hasErrors()) {
            Promise<Result> errorResult = Promise.promise(
                new Function0<Result>() {
                    public Result apply() {
                        return badRequest(home.render(
                            "Hi! This is Ode.", registrationForm));
                }
            });
            return errorResult;
        }
        Promise<Node> user = new User(registrationForm.get().email,
                                      registrationForm.get().password)
            .create();
        return user.map(new Function<Node, Result>() {
            public Result apply(Node node) {
                if (node == null) {
                    flash("error", "Something went wrong.");
                } else {
                    flash("success", "You've registered successfully.");
                }
                return redirect(routes.Application.home());
            }});
    }

    public static Result login() {
        return ok(login.render("Hi! Use the form below to log in.",
                               form(Login.class)));
    }

    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest(login.render("Do I know you?", loginForm));
        } else {
            session().clear();
            session("email", loginForm.get().email);
            return redirect(routes.Application.home());
        }
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
    public static Result features() {
        return ok(features.render("Hi! This is Ode's Feature Editor."));
    }

    // Forms

    public static class Login {
        public String email;
        public String password;

        public String validate() {
            if (email == "" || password == "") {
                return "You must provide input for all fields.";
            }
            return null;
        }
    }

    public static class Registration extends Login {}

}
