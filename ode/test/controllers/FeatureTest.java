package controllers;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.test.WithApplication;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.flash;
import static play.test.Helpers.status;

import models.Feature;


public class FeatureTest extends WithApplication {

    @Before
    public void setUp() {
        start(fakeApplication());
    }

    @Test
    public void featureSuccess() {
        Feature feature = new Feature("Foo", "complex");
        Result result = callAction(
            controllers.routes.ref.Application.feature(),
            fakeRequest()
                .withSession("email", "foo@bar.com")
                .withFormUrlEncodedBody(ImmutableMap.of(
                    "name", feature.name,
                    "type", feature.type)));
        assertEquals(status(result), 303);
        assertThat(flash(result).get("success")).isEqualTo(
            "Feature successfully created.");
        assert(feature.exists().get());
        feature.delete();
    }

    @Test
    public void featureCreateExisting() {
        Feature feature = new Feature("Foo", "complex").create().get();
        Result result = callAction(
            controllers.routes.ref.Application.feature(),
            fakeRequest()
                .withSession("email", "foo@bar.com")
                .withFormUrlEncodedBody(ImmutableMap.of(
                    "name", feature.name,
                    "type", feature.type)));
        assertEquals(status(result), 303);
        assertThat(flash(result).get("error")).isEqualTo(
            "Feature already exists.");
        feature.delete();
    }
}
