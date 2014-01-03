package neo4play;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.mvc.Http.Status;

import static org.junit.Assert.assertEquals;


public class Neo4jServiceTest {

    public Neo4jService neo4jService;
    public String resourceURL;

    @BeforeClass
    public static void setUpClass() {
        postCypherQuery(
            "CREATE (n:TestNode {name: 'node', " +
            "test: 'getLabeledNodeWithProperties'})");
        postCypherQuery(
            "CREATE (n:TestNode {name: 'node', " +
            "test: 'deleteLabeledNodeWithProperties'})");
        postCypherQuery("CREATE (n:GenericTestNode)");
        postCypherQuery("CREATE (n:GenericTestNode)");
        postCypherQuery(
            "CREATE (n:TestNode {name: 'node', " +
            "test: 'updateNodeProperties'})");
        postCypherQuery(
            "CREATE (n:TestNode {name: 'startNode', " +
            "test: 'createRelationship'})");
        postCypherQuery(
            "CREATE (n:TestNode {name: 'endNode', " +
            "test: 'createRelationship'})");
        postCypherQuery(
            "CREATE (s:TestNode {name: 'startNode', " +
            "test: 'getOutgoingRelationshipsByType'}), " +
            "(e:TestNode {name: 'endNode', " +
            "test: 'getOutgoingRelationshipsByType'}), " +
            "(s)-[:GENERIC]->(e)");
        postCypherQuery(
            "CREATE (n:TestNode {name: 'node', " +
            "test: 'getNodeURL'})");
        postCypherQuery(
            "CREATE (n:TestNode {name: 'node', " +
            "test: 'getNodeProperty'})");
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
            .get();
    }

    @Before
    public void setUp() {
        this.neo4jService = new Neo4jService();
        this.resourceURL = "/test";
    }

    @Test
    public void buildMatchQueryTest() {
        ObjectNode props = Json.newObject();
        assert(this.neo4jService.buildMatchQuery("TestNode", props)
           .startsWith("MATCH (n:TestNode) WHERE "));
    }

    @Test
    public void buildConjunctiveConstraintsTest() {
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "buildConjunctiveConstraints");
        assertEquals(
            this.neo4jService.buildConjunctiveConstraints(props),
            "n.name=\"node\" AND n.test=\"buildConjunctiveConstraints\"");
    }

    @Test
    public void getTest() {
        WS.Response getResponse = this.neo4jService.get(this.resourceURL)
            .get();
        String requestURI = getResponse.getUri().toString();
        String targetURI = this.neo4jService.rootURL + this.resourceURL;
        assertEquals(requestURI, targetURI);
    }

    @Test
    public void postTest() {
        ObjectNode content = Json.newObject();
        WS.Response postResponse = this.neo4jService.post(
            this.resourceURL, content).get();
        String requestURI = postResponse.getUri().toString();
        String targetURI = this.neo4jService.rootURL + this.resourceURL;
        assertEquals(requestURI, targetURI);
        String requestContentType = postResponse.getHeader("Content-Type");
        String targetContentType = this.neo4jService.contentType;
        assertEquals(requestContentType, targetContentType);
    }

    @Test
    public void getLabeledNodeWithPropertiesTest() {
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "getLabeledNodeWithProperties");
        WS.Response labeledNodeResponse = this.neo4jService
            .getLabeledNodeWithProperties("TestNode", props).get();
        assertEquals(labeledNodeResponse.getStatus(), Status.OK);
        assertEquals(labeledNodeResponse.asJson().get("data").size(), 1);
    }

    @Test
    public void createLabeledNodeWithPropertiesTest() {
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "createLabeledNodeWithProperties");
        WS.Response labeledNodeResponse = this.neo4jService
            .createLabeledNodeWithProperties("TestNode", props).get();
        assertEquals(labeledNodeResponse.getStatus(), Status.OK);
        assertEquals(labeledNodeResponse.asJson().get("data").size(), 1);
    }

    @Test
    public void deleteLabeledNodeWithPropertiesTest() {
        ObjectNode props = Json.newObject();
        props.put("name", "node");
        props.put("test", "deleteLabeledNodeWithProperties");
        WS.Response labeledNodeResponse = this.neo4jService
            .deleteLabeledNodeWithProperties("TestNode", props).get();
        assertEquals(labeledNodeResponse.getStatus(), Status.OK);
        assertEquals(labeledNodeResponse.asJson().get("data").size(), 0);
    }

    @Test
    public void getNodesByLabelTest() {
        WS.Response nodesByLabelResponse = this.neo4jService
            .getNodesByLabel("GenericTestNode").get();
        assertEquals(nodesByLabelResponse.getStatus(), Status.OK);
        assertEquals(nodesByLabelResponse.asJson().size(), 2);
    }

    @Test
    public void updateNodePropertiesTest() {
        ObjectNode oldProps = Json.newObject();
        oldProps.put("name", "node");
        oldProps.put("test", "updateNodeProperties");
        ObjectNode newProps = Json.newObject();
        newProps.put("name", "updated");
        newProps.put("test", "updated");
        WS.Response updatedNodeResponse = this.neo4jService
            .updateNodeProperties("TestNode", oldProps, newProps).get();
        assertEquals(updatedNodeResponse.getStatus(), Status.NO_CONTENT);
    }

    @Test
    public void createRelationshipTest() {
        ObjectNode startNodeProps = Json.newObject();
        startNodeProps.put("name", "startNode");
        startNodeProps.put("test", "createRelationship");
        ObjectNode endNodeProps = Json.newObject();
        endNodeProps.put("name", "endNode");
        endNodeProps.put("test", "createRelationship");
        WS.Response relationshipResponse = this.neo4jService
            .createRelationship(
                "TestNode", startNodeProps, "TestNode", endNodeProps,
                "GENERIC").get();
        assertEquals(relationshipResponse.getStatus(), Status.CREATED);
    }

    @Test
    public void getOutgoingRelationshipsByTypeTest() {
        ObjectNode startNodeProps = Json.newObject();
        startNodeProps.put("name", "startNode");
        startNodeProps.put("test", "getOutgoingRelationshipsByType");
        WS.Response outgoingRelationshipsResponse = this.neo4jService
            .getOutgoingRelationshipsByType(
                "TestNode", startNodeProps, "GENERIC").get();
        assertEquals(outgoingRelationshipsResponse.getStatus(), Status.OK);
        assertEquals(outgoingRelationshipsResponse.asJson().size(), 1);
    }

    @Test
    public void getNodePropertyTest() {
        WS.Response response = postCypherQuery(
            "MATCH (n:TestNode) WHERE n.name = 'node' AND " +
            "n.test = 'getNodeProperty' RETURN id(n)");
        String nodeID = response.asJson().get("data").get(0).get(0)
            .toString();
        String nodeURL = "http://localhost:7474/db/data/node/" + nodeID;
        String nodePropertyResponse = this.neo4jService
            .getNodeProperty(nodeURL, "name").get();
        assertEquals(nodePropertyResponse, "node");
        nodePropertyResponse = this.neo4jService
            .getNodeProperty(nodeURL, "test").get();
        assertEquals(nodePropertyResponse, "getNodeProperty");
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
        String nodeURLResponse = this.neo4jService
            .getNodeURL("TestNode", props).get();
        assertEquals(nodeURLResponse, nodeURL);
    }

}
