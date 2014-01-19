package neo4play;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.mvc.Http.Status;

import static org.fest.assertions.Assertions.assertThat;


public class Neo4jServiceTest {
    private static short ASYNC_TIMEOUT = 500;

    public String resourceURL;

    @BeforeClass
    public static void setUpClass() {
        postCypherQuery(
            "CREATE (n:TestNode {name: 'node', " +
            "test: 'getLabeledNodeWithProperties'})");
        postCypherQuery("CREATE (n:GenericTestNode)");
        postCypherQuery("CREATE (n:GenericTestNode)");
        postCypherQuery(
            "CREATE (n:TestNode {name: 'node', " +
            "test: 'updateNodeProperties'})");
        postCypherQuery(
            "CREATE (s:TestNode {name: 'startNode', " +
            "test: 'getOutgoingRelationshipsByType'}), " +
            "(e:TestNode {name: 'endNode', " +
            "test: 'getOutgoingRelationshipsByType'}), " +
            "(s)-[:GENERIC]->(e)");
        postCypherQuery(
            "CREATE (n:TestNode {name: 'node', " +
            "test: 'getNodeURL'})");
    }

    @AfterClass
    public static void tearDownClass() {
        postCypherQuery(
            "MATCH ()-[r:GENERIC]-() DELETE r");
        postCypherQuery("MATCH (n:TestNode) DELETE n");
        postCypherQuery("MATCH (n:GenericTestNode) DELETE n");
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
        this.resourceURL = "/test";
    }

    @Test
    public void buildNodeQueryTest() {
        ObjectNode props = Json.newObject();
        assertThat(Neo4jService.buildNodeQuery("TestNode", props)
           .startsWith("MATCH (n:TestNode) WHERE "));
    }

    @Test
    public void buildConjunctiveConstraintsTest() {
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "buildConjunctiveConstraints");
        assertThat(Neo4jService.buildConjunctiveConstraints(props))
            .isEqualTo(
                "n.name=\"node\" AND n.test=\"buildConjunctiveConstraints\"");
    }

    @Test
    public void postCypherQueryTest() {
        String query = "RETURN 0";
        WS.Response cypherQueryResponse = Neo4jService
            .postCypherQuery(query).get(ASYNC_TIMEOUT);
        assertThat(cypherQueryResponse.getStatus()).isEqualTo(Status.OK);
    }

    @Test
    public void postCypherQueryWithParamsTest() {
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "postCypherQueryWithParams");
        String query = "CREATE (n:TestNode { props }) RETURN n";
        WS.Response cypherQueryWithParamsResponse = Neo4jService
            .postCypherQueryWithParams(query, props).get(ASYNC_TIMEOUT);
        assertThat(cypherQueryWithParamsResponse.getStatus())
            .isEqualTo(Status.OK);
    }

    @Test
    public void getTest() {
        WS.Response getResponse = Neo4jService.get(this.resourceURL)
            .get(ASYNC_TIMEOUT);
        String requestURI = getResponse.getUri().toString();
        String targetURI = Neo4jService.rootURL + this.resourceURL;
        assertThat(requestURI).isEqualTo(targetURI);
    }

    @Test
    public void postTest() {
        ObjectNode content = Json.newObject();
        WS.Response postResponse = Neo4jService.post(
            this.resourceURL, content).get(ASYNC_TIMEOUT);
        String requestURI = postResponse.getUri().toString();
        String targetURI = Neo4jService.rootURL + this.resourceURL;
        assertThat(requestURI).isEqualTo(targetURI);
    }

    @Test
    public void getNodeURLTest() {
        WS.Response response = postCypherQuery(
            "MATCH (n:TestNode) WHERE n.name = 'node' AND " +
            "n.test = 'getNodeURL' RETURN id(n)");
        String nodeID = response.asJson().get("data").get(0).get(0)
            .toString();
        String nodeURL = "http://localhost:7474/db/data/node/" + nodeID;
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "getNodeURL");
        String nodeURLResponse = Neo4jService
            .getNodeURL("TestNode", props).get(ASYNC_TIMEOUT);
        assertThat(nodeURLResponse).isEqualTo(nodeURL);
    }

    @Test
    public void getLabeledNodeWithPropertiesTest() {
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "getLabeledNodeWithProperties");
        WS.Response labeledNodeResponse = Neo4jService
            .getLabeledNodeWithProperties("TestNode", props)
            .get(ASYNC_TIMEOUT);
        assertThat(labeledNodeResponse.getStatus()).isEqualTo(Status.OK);
        assertThat(labeledNodeResponse.asJson().get("data").size())
            .isEqualTo(1);
    }

    @Test
    public void createLabeledNodeWithPropertiesTest() {
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "createLabeledNodeWithProperties");
        WS.Response labeledNodeResponse = Neo4jService
            .createLabeledNodeWithProperties("TestNode", props)
            .get(ASYNC_TIMEOUT);
        assertThat(labeledNodeResponse.getStatus()).isEqualTo(Status.OK);
        assertThat(labeledNodeResponse.asJson().get("data").size())
            .isEqualTo(1);
    }

    @Test
    public void getNodesByLabelTest() {
        WS.Response nodesByLabelResponse = Neo4jService
            .getNodesByLabel("GenericTestNode").get(ASYNC_TIMEOUT);
        assertThat(nodesByLabelResponse.getStatus()).isEqualTo(Status.OK);
        assertThat(nodesByLabelResponse.asJson().size()).isEqualTo(2);
    }

    @Test
    public void updateNodePropertiesTest() {
        ObjectNode oldProps = Json.newObject();
        oldProps.put("name", "node");
        oldProps.put("test", "updateNodeProperties");
        ObjectNode newProps = Json.newObject();
        newProps.put("name", "updated");
        newProps.put("test", "updated");
        WS.Response updatedNodeResponse = Neo4jService
            .updateNodeProperties("TestNode", oldProps, newProps)
            .get(ASYNC_TIMEOUT);
        assertThat(updatedNodeResponse.getStatus())
            .isEqualTo(Status.NO_CONTENT);
    }

    @Test
    public void getOutgoingRelationshipsByTypeTest() {
        ObjectNode startNodeProps = Json.newObject();
        startNodeProps.put("name", "startNode");
        startNodeProps.put("test", "getOutgoingRelationshipsByType");
        WS.Response outgoingRelationshipsResponse = Neo4jService
            .getOutgoingRelationshipsByType(
                "TestNode", startNodeProps, "GENERIC").get(ASYNC_TIMEOUT);
        assertThat(outgoingRelationshipsResponse.getStatus())
            .isEqualTo(Status.OK);
        assertThat(outgoingRelationshipsResponse.asJson().size())
            .isEqualTo(1);
    }

}
