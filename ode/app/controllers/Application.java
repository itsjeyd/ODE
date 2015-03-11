// Application.java --- Controller that handles display of home page, login page, and logout.

// Copyright (C) 2013-2015  Tim Krones

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package controllers;

import controllers.Auth.RegistrationForm;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
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
