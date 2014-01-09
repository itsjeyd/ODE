package controllers;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;

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
        return user.exists().flatMap(
            new Function<Boolean, Promise<Result>>() {
                public Promise<Result> apply(Boolean exists) {
                    if (exists) {
                        return Promise.promise(new Function0<Result>() {
                            public Result apply() {
                                flash("error", "User already exists.");
                                return redirect(routes.Application.home());
                            }
                        });
                    } else {
                        Promise<User> newUser = user.create();
                        return newUser.map(new Function<User, Result>() {
                            public Result apply(User user) {
                                if (user == null) {
                                    flash("error", "Registration failed.");
                                } else {
                                    flash("success",
                                          "Registration successful.");
                                }
                                return redirect(routes.Application.home());
                            }
                        });
                    }
                }
            });
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

    public static class RegistrationForm extends Login {}

}
