package controllers;

import play.mvc.*;
import play.mvc.Http.*;

public class Secured extends Security.Authenticator {

    @Override
    public String getUsername(Context context) {
        return context.session().get("username");
    }

    @Override
    public Result onUnauthorized(Context context) {
        return redirect(routes.Application.login());
    }

}
