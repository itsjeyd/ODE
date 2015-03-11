// Values.java --- Controller that handles requests for operating on values.

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

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.nodes.Value;
import play.Routes;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;


public class Values extends Controller {

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateName(final String name) {
        final ObjectNode newProps = (ObjectNode) request().body().asJson();
        newProps.retain("name");
        Promise<Boolean> nameTaken = Value.nodes.exists(newProps);
        Promise<Boolean> updated = nameTaken.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean nameTaken) {
                    if (nameTaken) {
                        return Promise.pure(false);
                    }
                    ObjectNode oldProps = Json.newObject();
                    oldProps.put("name", name);
                    return Value.nodes.update(oldProps, newProps);
                }
            });
        return updated.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        result.put("message", "Name successfully updated.");
                        return ok(result);
                    }
                    result.put("message", "Name not updated.");
                    return badRequest(result);
                }
            });
    }

}
