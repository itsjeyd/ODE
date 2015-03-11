// Global.java --- Define global settings for ODE.

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

import play.GlobalSettings;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.SimpleResult;
import play.mvc.Results;
import views.html.error;
import views.html.missing;


public class Global extends GlobalSettings {

    @Override
    public Promise<SimpleResult> onError(
        Http.RequestHeader request, Throwable t) {
        return Promise.<SimpleResult>pure(
            Results.internalServerError(error.render()));
    }

    @Override
    public Promise<SimpleResult> onHandlerNotFound(
        Http.RequestHeader request) {
        return Promise.<SimpleResult>pure(
            Results.notFound(missing.render()));
    }

    @Override
    public Promise<SimpleResult> onBadRequest(
        Http.RequestHeader request, String error) {
        return Promise.<SimpleResult>pure(
            Results.badRequest(missing.render()));
    }

}
