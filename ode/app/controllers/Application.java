package controllers;

import play.*;
import play.data.*;
import play.mvc.*;

import views.html.*;

import static play.data.Form.*;


public class Application extends Controller {

    public static Result home() {
        return ok(home.render("Hi! This is Ode."));
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
            return redirect(routes.Application.home());
        }
    }

    public static Result rules() {
        return ok(rules.render("Hi! This is Ode's Rule Browser."));
    }

    public static Result rule(int id) {
        return ok(rule.render("Hi! You are looking at rule " + id + "."));
    }

    public static Result search() {
        return ok(search.render("Hi! This is Ode's Search Interface."));
    }

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

}
