// FeaturesTest.java --- Tests for Features controller.

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

import com.fasterxml.jackson.databind.JsonNode;
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

import models.nodes.Feature;


public class FeaturesTest extends WithApplication {
    private static short ASYNC_TIMEOUT = 500;

    @BeforeClass
    public static void setUpClass() {
        postCypherQuery(
            "CREATE (n:Feature {name: 'ExistingFeature', " +
            "type: 'atomic', description: '...', " +
            "uuid: '008da956-fcdb-4a62-adef-c96d8cea3f02'})");
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
            controllers.routes.ref.Features.features(),
            fakeRequest().withSession("username", "user@example.com"));
        assertThat(status(result)).isEqualTo(Status.OK);
        assertThat(contentAsString(result).contains("ExistingFeature"));
    }

    @Test
    public void createFeatureSuccess() {
        ObjectNode feature = Json.newObject();
        feature.put("name", "NonExistingFeature");
        feature.put("description", "...");
        feature.put("type", "complex");
        feature.put("uuid", "e9612ab6-0ed8-4bcc-8930-464a7745125d");
        Result result = callAction(
            controllers.routes.ref.Features.create(),
            fakeRequest().withSession("username", "user@example.com")
                         .withJsonBody(feature));
        assertThat(status(result)).isEqualTo(Status.OK);
        assert(Feature.nodes.exists(feature).get(ASYNC_TIMEOUT));
    }

    @Test
    public void createFeatureExisting() {
        ObjectNode feature = Json.newObject();
        feature.put("name", "ExistingFeature");
        feature.put("description", "...");
        feature.put("type", "atomic");
        feature.put("uuid", "008da956-fcdb-4a62-adef-c96d8cea3f02");
        Result result = callAction(
            controllers.routes.ref.Features.create(),
            fakeRequest().withSession("username", "user@example.com")
                         .withJsonBody(feature));
        assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);
    }

    @Test
    public void updateNameTest() {
    }

    @Test
    public void updateDescriptionTest() {
    }

    @Test
    public void updateTypeTest() {
    }

    @Test
    public void addTargetTest() {
    }

    @Test
    public void removeTargetTest() {
    }

    @Test
    public void deleteTest() {
    }

}
