package controllers;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.WithApplication;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.flash;
import static play.test.Helpers.status;

import models.AtomicFeature;
import models.ComplexFeature;
import models.Feature;


public class FeaturesTest extends WithApplication {
    private short ASYNC_TIMEOUT = 500;

    @Before
    public void setUp() {
        start(fakeApplication());
    }

    @Test
    public void featureSuccess() {
        Feature feature = new ComplexFeature(
            "NonExistingFeature", "This is not a description.");
        Result result = callAction(
            controllers.routes.ref.Features.createFeature(),
            fakeRequest()
                .withSession("email", "foo@bar.com")
                .withFormUrlEncodedBody(ImmutableMap.of(
                    "name", feature.name,
                    "type", feature.getType(),
                    "description", feature.getDescription())));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(flash(result).get("success")).isEqualTo(
            "Feature successfully created.");
        assert(feature.exists().get(ASYNC_TIMEOUT));
        feature.delete().get(ASYNC_TIMEOUT);
    }

    @Test
    public void featureCreateExisting() {
        Feature feature = new AtomicFeature(
            "ExistingFeature", "This is not a description.")
            .create().get(ASYNC_TIMEOUT);
        Result result = callAction(
            controllers.routes.ref.Features.createFeature(),
            fakeRequest()
                .withSession("email", "foo@bar.com")
                .withFormUrlEncodedBody(ImmutableMap.of(
                    "name", feature.name,
                    "type", feature.getType(),
                    "description", feature.getDescription())));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(flash(result).get("error")).isEqualTo(
            "Feature already exists.");
        feature.delete().get(ASYNC_TIMEOUT);
    }
}
