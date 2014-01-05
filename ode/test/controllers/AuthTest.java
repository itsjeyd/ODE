package controllers;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.mvc.Http.Status;
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

import models.User;


public class AuthTest extends WithApplication {
    private short ASYNC_TIMEOUT = 500;

    @Before
    public void setUp() {
        start(fakeApplication());
    }

    @Test
    public void authenticateSuccess() {
        User user = new User("foo@bar.com", "password").create()
            .get(ASYNC_TIMEOUT);
        Result result = callAction(
            controllers.routes.ref.Auth.authenticate(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", user.username,
                "password", user.password)));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(session(result).get("email")).isEqualTo(user.username);
        user.delete().get(ASYNC_TIMEOUT);
    }

    @Test
    public void authenticateFailure() {
        Result result = callAction(
            controllers.routes.ref.Auth.authenticate(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", "",
                "password", "")));
        assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);
        assertNull(session(result).get("email"));
    }

    @Test
    public void authenticateUnknownUser() {
        Result result = callAction(
            controllers.routes.ref.Auth.authenticate(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", "foo@bar.com",
                "password", "password")));
        assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);
        assertNull(session(result).get("email"));
    }

    @Test
    public void logoutSuccess() {
        Result result = callAction(
            controllers.routes.ref.Application.logout(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertNull(session(result).get("email"));
        assertThat(flash(result).get("success")).isEqualTo(
            "Alright. See you around.");
    }

    @Test
    public void authenticated() {
        Result rulesResult = callAction(
            controllers.routes.ref.Application.rules(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertThat(status(rulesResult)).isEqualTo(Status.OK);
        Result ruleResult = callAction(
            controllers.routes.ref.Application.rule(1),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertThat(status(ruleResult)).isEqualTo(Status.OK);
        Result searchResult = callAction(
            controllers.routes.ref.Application.search(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertThat(status(searchResult)).isEqualTo(Status.OK);
        Result featuresResult = callAction(
            controllers.routes.ref.Features.list(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertThat(status(featuresResult)).isEqualTo(Status.OK);
        Result logoutResult = callAction(
            controllers.routes.ref.Application.logout(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertThat(status(logoutResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", logoutResult)).isEqualTo(
            routes.Application.login().url());
    }

    @Test
    public void notAuthenticated() {
        Result rulesResult = callAction(
            controllers.routes.ref.Application.rules(),
            fakeRequest());
        assertThat(status(rulesResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", rulesResult)).isEqualTo(
            routes.Application.login().url());
        Result ruleResult = callAction(
            controllers.routes.ref.Application.rule(1),
            fakeRequest());
        assertThat(status(ruleResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", ruleResult)).isEqualTo(
                routes.Application.login().url());
        Result searchResult = callAction(
            controllers.routes.ref.Application.search(),
            fakeRequest());
        assertThat(status(searchResult)).isEqualTo(Status.SEE_OTHER);
        assertThat(header("Location", searchResult)).isEqualTo(
                routes.Application.login().url());
        Result featuresResult = callAction(
            controllers.routes.ref.Features.list(),
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
        User user = new User("foo@bar.com", "password");
        Result result = callAction(
            controllers.routes.ref.Auth.register(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", user.username,
                "password", user.password)));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(flash(result).get("success")).isEqualTo(
            "Registration successful.");
        assert(user.exists().get(ASYNC_TIMEOUT));
        user.delete().get(ASYNC_TIMEOUT);
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
        User user = new User("foo@bar.com", "password").create()
            .get(ASYNC_TIMEOUT);
        Result result = callAction(
            controllers.routes.ref.Auth.register(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", user.username,
                "password", user.password)));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(flash(result).get("error")).isEqualTo(
            "User already exists.");
        user.delete().get(ASYNC_TIMEOUT);
    }

}
