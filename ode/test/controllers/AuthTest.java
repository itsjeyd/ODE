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

}
