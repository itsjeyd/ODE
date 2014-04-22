package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import views.html.search;


public class Search extends Controller {

    @Security.Authenticated(Secured.class)
    public static Result search() {
        return ok(search.render());
    }

}
