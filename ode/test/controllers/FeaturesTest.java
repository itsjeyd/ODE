package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.collect.ImmutableMap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.libs.WS;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.WithApplication;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.flash;
import static play.test.Helpers.status;

import models.nodes.AtomicFeature;
import models.nodes.ComplexFeature;
import models.nodes.Feature;


public class FeaturesTest extends WithApplication {
    private static short ASYNC_TIMEOUT = 500;

    @BeforeClass
    public static void setUpClass() {
        postCypherQuery(
            "CREATE (n:Feature {name: 'ExistingFeature', " +
            "type: 'atomic', description: '...'})");
    }

    @AfterClass
    public static void tearDownClass() {
        postCypherQuery(
            "MATCH (n:Feature {name: 'NonExistingFeature', " +
            "type: 'complex', description: '...'}) DELETE n");
        postCypherQuery(
            "MATCH (n:Feature {name: 'ExistingFeature', " +
            "type: 'atomic', description: '...'}) DELETE n");
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
    public void listTest() {
        Result result = callAction(
            controllers.routes.ref.Features.list(),
            fakeRequest().withSession("username", "user@example.com"));
        assertThat(status(result)).isEqualTo(Status.OK);
        assertThat(contentAsString(result).contains("ExistingFeature"));
    }

    @Test
    public void createFeatureSuccess() {
        Feature feature = new ComplexFeature("NonExistingFeature", "...");
        Result result = callAction(
            controllers.routes.ref.Features.createFeature(),
            fakeRequest()
                .withSession("username", "user@example.com")
                .withFormUrlEncodedBody(ImmutableMap.of(
                    "name", feature.name,
                    "type", feature.getType(),
                    "description", feature.getDescription())));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(flash(result).get("success")).isEqualTo(
            "Feature successfully created.");
        assert(feature.exists().get(ASYNC_TIMEOUT));
    }

    @Test
    public void createFeatureExisting() {
        Feature feature = new AtomicFeature("ExistingFeature", "...");
        Result result = callAction(
            controllers.routes.ref.Features.createFeature(),
            fakeRequest()
                .withSession("username", "user@example.com")
                .withFormUrlEncodedBody(ImmutableMap.of(
                    "name", feature.name,
                    "type", feature.getType(),
                    "description", feature.getDescription())));
        assertThat(status(result)).isEqualTo(Status.SEE_OTHER);
        assertThat(flash(result).get("error")).isEqualTo(
            "Feature already exists.");
    }

    @Test
    public void updateFeatureNameTest() {
    }

    @Test
    public void updateFeatureDescriptionTest() {
    }

    @Test
    public void updateFeatureTypeTest() {
    }

    @Test
    public void addTargetsTest() {
    }

    @Test
    public void deleteTargetTest() {
    }

    @Test
    public void deleteFeatureTest() {
    }

}
