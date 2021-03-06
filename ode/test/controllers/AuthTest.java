// AuthTest.java --- Tests for Auth controller.

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
import com.google.common.collect.ImmutableMap;
import models.nodes.User;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.libs.WS;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.WithApplication;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.flash;
import static play.test.Helpers.header;
import static play.test.Helpers.session;
import static play.test.Helpers.status;


public class AuthTest extends WithApplication {
    private static short ASYNC_TIMEOUT = 500;

    @BeforeClass
    public static void setUpClass() {
        postCypherQuery(
            "CREATE (n:User {username: 'user@example.com', " +
            "password: 'password'})");
        postCypherQuery(
            "CREATE (n:User {username: 'existing@example.com', " +
            "password: 'password'})");
        postCypherQuery(
            "CREATE (n:Rule {name: 'test', description: '...', " +
            "uuid: 'e9612ab6-0ed8-4bcc-8930-464a7745125d'})");
    }

    @AfterClass
    public static void tearDownClass() {
        postCypherQuery(
            "MATCH (n:User {username: 'user@example.com', " +
            "password: 'password'}) DELETE n");
        postCypherQuery(
            "MATCH (n:User {username: 'new@example.com', " +
            "password: 'password'}) DELETE n");
        postCypherQuery(
            "MATCH (n:User {username: 'existing@example.com', " +
            "password: 'password'}) DELETE n");
        postCypherQuery(
            "MATCH (n:Rule {name: 'test', description: '...', " +
            "uuid: 'e9612ab6-0ed8-4bcc-8930-464a7745125d'}) DELETE n");
    }

    private static WS.Response postCypherQuery(String query) {
        String cypherURL = "http://localhost:7474/db/data/cypher";
        String contentType = "application/json";
        ObjectNode content = Json.newObject();
        content.put("query", query);
        return WS.url(cypherURL).setContentType(contentType).post(content)
            .get(ASYNC_TIMEOUT);
    }

    @Before
    public void setUp() {
        start(fakeApplication());
    }

    @Test
    public void authenticateSuccess() {
        Result result = callAction(
            controllers.routes.ref.Auth.authenticate(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", "user@example.com",
                "password", "password")));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(session(result).get("username"))
            .isEqualTo("user@example.com");
    }

    @Test
    public void authenticateFailure() {
        Result result = callAction(
            controllers.routes.ref.Auth.authenticate(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", "",
                "password", "")));
        assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);
        assertNull(session(result).get("username"));
    }

    @Test
    public void authenticateUnknownUser() {
        Result result = callAction(
            controllers.routes.ref.Auth.authenticate(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", "unknown@example.com",
                "password", "password")));
        assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);
        assertNull(session(result).get("username"));
    }

    @Test
    public void logoutSuccess() {
        Result result = callAction(
            controllers.routes.ref.Application.logout(),
            fakeRequest().withSession("username", "user@example.com"));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertNull(session(result).get("username"));
        assertThat(flash(result).get("success")).isEqualTo(
            "Alright. See you around.");
    }

    @Test
    public void authenticated() {
        Result rulesResult = callAction(
            controllers.routes.ref.Rules.browse(),
            fakeRequest().withSession("username", "user@example.com"));
        assertThat(status(rulesResult)).isEqualTo(Status.OK);
        Result ruleResult = callAction(
            controllers.routes.ref.Rules.details("test"),
            fakeRequest().withSession("username", "user@example.com"));
        assertThat(status(ruleResult)).isEqualTo(Status.OK);
        Result searchResult = callAction(
            controllers.routes.ref.Search.search(),
            fakeRequest().withSession("username", "user@example.com"));
        assertThat(status(searchResult)).isEqualTo(Status.OK);
        Result featuresResult = callAction(
            controllers.routes.ref.Features.features(),
            fakeRequest().withSession("username", "user@example.com"));
        assertThat(status(featuresResult)).isEqualTo(Status.OK);
        Result logoutResult = callAction(
            controllers.routes.ref.Application.logout(),
            fakeRequest().withSession("username", "user@example.com"));
        assertThat(status(logoutResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", logoutResult)).isEqualTo(
            routes.Application.login().url());
    }

    @Test
    public void notAuthenticated() {
        Result rulesResult = callAction(
            controllers.routes.ref.Rules.browse(),
            fakeRequest());
        assertThat(status(rulesResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", rulesResult)).isEqualTo(
            routes.Application.login().url());
        Result ruleResult = callAction(
            controllers.routes.ref.Rules.details("test"),
            fakeRequest());
        assertThat(status(ruleResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", ruleResult)).isEqualTo(
                routes.Application.login().url());
        Result searchResult = callAction(
            controllers.routes.ref.Search.search(),
            fakeRequest());
        assertThat(status(searchResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", searchResult)).isEqualTo(
                routes.Application.login().url());
        Result featuresResult = callAction(
            controllers.routes.ref.Features.features(),
            fakeRequest());
        assertThat(status(featuresResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", featuresResult)).isEqualTo(
            routes.Application.login().url());
        Result logoutResult = callAction(
            controllers.routes.ref.Application.logout(),
            fakeRequest());
        assertThat(status(logoutResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", logoutResult)).isEqualTo(
                routes.Application.login().url());
    }

    @Test
    public void registerSuccess() {
        String username = "new@example.com";
        String password = "password";
        Result result = callAction(
            controllers.routes.ref.Auth.register(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", username,
                "password", password)));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(flash(result).get("success")).isEqualTo(
            "Registration successful.");
        ObjectNode user = Json.newObject();
        user.put("username", username);
        user.put("password", password);
        assert(User.nodes.exists(user).get(ASYNC_TIMEOUT));
    }

    @Test
    public void registerFailure() {
        Result result = callAction(
            controllers.routes.ref.Auth.register(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", "",
                "password", "")));
        assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);
    }

    @Test
    public void registerExistingUser() {
        String username = "existing@example.com";
        String password = "password";
        Result result = callAction(
            controllers.routes.ref.Auth.register(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", username,
                "password", password)));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(flash(result).get("error")).isEqualTo(
            "Registration failed.");
    }

}
