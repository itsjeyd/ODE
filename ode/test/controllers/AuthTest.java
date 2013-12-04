package controllers;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.test.WithApplication;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.flash;
import static play.test.Helpers.header;
import static play.test.Helpers.session;
import static play.test.Helpers.status;


public class AuthTest extends WithApplication {

    @Before
    public void setUp() {
        start(fakeApplication());
    }

    @Test
    public void authenticateSuccess() {
        Result result = callAction(
            controllers.routes.ref.Application.authenticate(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", "foo@bar.com",
                "password", "password")));
        assertEquals(status(result), 303);
        assertEquals(session(result).get("email"), "foo@bar.com");
    }

    @Test
    public void authenticateFailure() {
        Result result = callAction(
            controllers.routes.ref.Application.authenticate(),
            fakeRequest().withFormUrlEncodedBody(ImmutableMap.of(
                "email", "",
                "password", "")));
        assertEquals(status(result), 400);
        assertNull(session(result).get("email"));
    }

    @Test
    public void logoutSuccess() {
        Result result = callAction(
            controllers.routes.ref.Application.logout(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertEquals(status(result), 303);
        assertNull(session(result).get("email"));
        assertThat(flash(result).get("success")).isEqualTo(
            "Alright. See you around.");
    }

    @Test
    public void authenticated() {
        Result rulesResult = callAction(
            controllers.routes.ref.Application.rules(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertEquals(status(rulesResult), 200);
        Result ruleResult = callAction(
            controllers.routes.ref.Application.rule(1),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertEquals(status(ruleResult), 200);
        Result searchResult = callAction(
            controllers.routes.ref.Application.search(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertEquals(status(searchResult), 200);
        Result featuresResult = callAction(
            controllers.routes.ref.Application.features(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertEquals(status(featuresResult), 200);
        Result logoutResult = callAction(
            controllers.routes.ref.Application.logout(),
            fakeRequest().withSession("email", "foo@bar.com"));
        assertEquals(status(logoutResult), 303);
        assertEquals(
            header("Location", logoutResult),
            routes.Application.login().url());
    }

    @Test
    public void notAuthenticated() {
        Result rulesResult = callAction(
            controllers.routes.ref.Application.rules(),
            fakeRequest());
        assertEquals(status(rulesResult), 303);
        assertEquals(
            header("Location", rulesResult),
            routes.Application.login().url());
        Result ruleResult = callAction(
            controllers.routes.ref.Application.rule(1),
            fakeRequest());
        assertEquals(status(ruleResult), 303);
        assertEquals(
            header("Location", ruleResult),
            routes.Application.login().url());
        Result searchResult = callAction(
            controllers.routes.ref.Application.search(),
            fakeRequest());
        assertEquals(status(searchResult), 303);
        assertEquals(
            header("Location", searchResult),
            routes.Application.login().url());
        Result featuresResult = callAction(
            controllers.routes.ref.Application.features(),
            fakeRequest());
        assertEquals(status(featuresResult), 303);
        assertEquals(
            header("Location", featuresResult),
            routes.Application.login().url());
        Result logoutResult = callAction(
            controllers.routes.ref.Application.logout(),
            fakeRequest());
        assertEquals(status(logoutResult), 303);
        assertEquals(
            header("Location", logoutResult),
            routes.Application.login().url());
    }

}
