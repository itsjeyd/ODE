package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {

    public static Result home() {
        return ok(home.render("Hi! This is Ode."));
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

}
