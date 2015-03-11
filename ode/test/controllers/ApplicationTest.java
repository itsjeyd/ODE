// ApplicationTest.java --- Tests for Application controller.

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

import org.junit.*;

import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.WithApplication;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.status;


public class ApplicationTest extends WithApplication {

    @Test
    public void homeTest() {
        Result result = callAction(
            controllers.routes.ref.Application.home(), fakeRequest());
        assertThat(status(result)).isEqualTo(Status.OK);
        assertThat(contentType(result)).isEqualTo("text/html");
        assertThat(contentAsString(result)).contains("create an account");
    }

}
