package controllers;

import java.util.List;

import play.Routes;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;

import models.Value;

import static play.data.Form.form;


public class Values extends Controller {

    @Security.Authenticated(Secured.class)
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
            Routes.javascriptRouter(
                "jsValueRoutes",
                controllers.routes.javascript.Values.renameValue()));
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> renameValue(final String name) {
        DynamicForm nameForm = form().bindFromRequest();
        final String newName = nameForm.get("name");
        Promise<List<Value>> values = Value.all();
        Promise<Boolean> nameAlreadyTaken = values.map(
            new Function<List<Value>, Boolean>() {
                public Boolean apply(List<Value> values) {
                    Boolean nameAlreadyTaken = false;
                    for (Value value: values) {
                        if (value.name.equals(newName)) {
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
                        Promise<Boolean> nameUpdated = new Value(
                            name).updateName(newName);
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

}
