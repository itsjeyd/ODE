package controllers;

import play.data.Form;
import play.mvc.Content;
import play.mvc.Controller;
import play.mvc.Result;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import controllers.Application.Login;
import models.User;
import views.html.home;
import views.html.login;

import static play.data.Form.form;


public class Auth extends Controller {

    public static Promise<Result> register() {
        final Form<RegistrationForm> registrationForm =
            form(RegistrationForm.class).bindFromRequest();
        if (registrationForm.hasErrors()) {
            return Promise.promise(
                new ErrorResult(home.render(registrationForm)));
        }
        Promise<Tuple<Option<User>, Boolean>> result = new User(
            registrationForm.get().email,
            registrationForm.get().password).getOrCreate();
        return result.map(
            new Function<Tuple<Option<User>, Boolean>, Result>() {
                public Result apply(Tuple<Option<User>, Boolean> result) {
                    Boolean created = result._2;
                    // Three cases:
                    // 1. User was created successfully: Some<User>, true
                    // 2. User already exists: Some<User>, false
                    // 3. User could not be created: None<User>, false
                    if (created) {
                        flash("success", "Registration successful.");
                    } else {
                        Option<User> user = result._1;
                        if (user.isDefined()) {
                            flash("error", "User already exists.");
                        } else {
                            flash("error", "Registration failed.");
                        }
                    }
                    return redirect(routes.Application.home());
                }
            });
    }

    public static Promise<Result> authenticate() {
        final Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return Promise.promise(new ErrorResult(login.render(loginForm)));
        }
        Promise<Option<User>> user = new User(
            loginForm.get().email,
            loginForm.get().password).get();
        return user.map(new Function<Option<User>, Result>() {
            public Result apply(Option<User> user) {
                if (user.isDefined()) {
                    session().clear();
                    session("username", user.get().username);
                    flash("success", "Login successful.");
                    return redirect(routes.Application.home());
                } else {
                    flash("error", "Login failed: Unknown user.");
                    return badRequest(login.render(loginForm));
                }
            }
        });
    }

    private static class ErrorResult implements Function0<Result> {
        private Content content;
        public ErrorResult(Content content) {
            this.content = content;
        }
        public Result apply() {
            return badRequest(content);
        }
    }

    public static class RegistrationForm extends Login {}

}
