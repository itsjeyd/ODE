package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import controllers.Auth.RegistrationForm;
import views.html.*;

import static play.data.Form.*;


public class Application extends Controller {

    public static Result home() {
        return ok(home.render(form(RegistrationForm.class)));
    }

    public static Result login() {
        return ok(login.render(form(Login.class)));
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
    public static Result rule(String name) {
        return ok(rule.render("Hi! You are looking at rule " + name + "."));
    }

    @Security.Authenticated(Secured.class)
    public static Result search() {
        return ok(search.render("Hi! This is Ode's Search Interface."));
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

}
