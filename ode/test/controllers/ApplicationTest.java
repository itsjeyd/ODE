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
