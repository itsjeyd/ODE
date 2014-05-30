package neo4play;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.RelationshipType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import models.nodes.LabeledNodeWithProperties;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.WS;
import utils.StringUtils;


public class Neo4jService {
    protected static String rootURL = "http://localhost:7474/db/data";
    protected static String contentType = "application/json";

    private static String extendRootURL(String suffix) {
        if (suffix.startsWith("/")) {
            return rootURL + suffix;
        } else {
            return rootURL + "/" + suffix;
        }
    }

    // Cypher utils

    protected static String buildNodeQuery(String nodeLabel, JsonNode props) {
        return "MATCH (n:" + nodeLabel + ") WHERE "
            + buildConjunctiveConstraints(props);
    }

    protected static String buildConjunctiveConstraints(JsonNode props) {
        return buildConjunctiveConstraints("n", props);
    }

    protected static String buildConjunctiveConstraints(
        String varName, JsonNode props) {
        List<String> constraints = new ArrayList<String>();
        for (Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
             fields.hasNext(); ) {
            constraints.add(
                String.format("%s.%s", varName, fields.next().toString()));
        }
        return StringUtils.join(constraints, " AND ");
    }

    protected static Promise<WS.Response> postCypherQuery(String query) {
        ObjectNode content = Json.newObject();
        content.put("query", query);
        return post("/cypher", content);
    }

    protected static Promise<WS.Response> postCypherQueryWithParams(
        String query, JsonNode props) {
        ObjectNode params = Json.newObject();
        params.put("props", props);
        ObjectNode content = Json.newObject();
        content.put("query", query);
        content.put("params", params);
        return post("/cypher", content);
    }

    // GET, POST, DELETE

    protected static Promise<WS.Response> get(String resourceURL) {
        String fullURL = extendRootURL(resourceURL);
        return WS.url(fullURL).get();
    }

    protected static Promise<WS.Response> post(
        String resourceURL, JsonNode content) {
        String fullURL = extendRootURL(resourceURL);
        return WS.url(fullURL).setContentType(contentType).
            post(content);
    }

    protected static Promise<WS.Response> delete(String url) {
        return WS.url(url).delete();
    }

    // Node utils

    protected static Promise<WS.Response> getNodeByURL(String nodeURL) {
        return WS.url(nodeURL).get();
    }

    // API for external clients (managers)

    public static Promise<String> getNodeURL(
        String label, JsonNode props) {
        Promise<WS.Response> nodeResponse = getLabeledNodeWithProperties(
            label, props);
        return nodeResponse.map(new NodeURLFunction());
    }

    public static Promise<WS.Response> getLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = buildNodeQuery(label, props) + " RETURN n";
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> getNodesByLabel(String label) {
        return get("/label/" + label + "/nodes");
    }

    public static Promise<WS.Response> getNodeProperties(String nodeURL) {
        String fullURL = nodeURL + "/properties";
        return WS.url(fullURL).get();
    }

    public static Promise<WS.Response> getOutgoingRelationshipsByType(
        String startNodeLabel, JsonNode startNodeProps,
        final String relationshipType) {
        Promise<String> startNodeURL = getNodeURL(startNodeLabel,
                                                  startNodeProps);
        return startNodeURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String nodeURL) {
                    String fullURL = nodeURL + "/relationships/out/"
                        + relationshipType;
                    return WS.url(fullURL).get();
        }});
    }

    public static Promise<WS.Response> getRelationshipTarget(
        LabeledNodeWithProperties startNode, RelationshipType relType,
        JsonNode relationshipProperties) {
        String startNodeProps = buildConjunctiveConstraints(
            "s", startNode.jsonProperties);
        String relProps = buildConjunctiveConstraints(
            "r", relationshipProperties);
        String query = String.format(
            "MATCH (s:%s)-[r:%s]-(e) WHERE %s AND %s RETURN e",
            startNode.getLabel(), relType.name(), startNodeProps, relProps);
        return postCypherQuery(query);
    }

    public static Promise<List<WS.Response>> getRelationshipTargets(
        String nodeLabel, JsonNode nodeProps, String relationshipType) {
        Promise<WS.Response> response = getOutgoingRelationshipsByType(
            nodeLabel, nodeProps, relationshipType);
        Promise<List<String>> targetURLs = response.map(
            new TargetURLsFunction());
        return targetURLs.flatMap(new NodesByURLFunction());
    }

    public static Promise<WS.Response> fuzzyFindTargetsAnyDepth(
        LabeledNodeWithProperties startNode, String targetNodeLabel,
        String targetPropName, String targetPropValue) {
        String startNodeProps = buildConjunctiveConstraints(
            "s", startNode.jsonProperties);
        String query = String.format(
            "MATCH (s:%s)-[*]->(t:%s) WHERE %s AND lower(t.%s) =~ lower('.*%s.*') RETURN t",
            startNode.getLabel(), targetNodeLabel, startNodeProps,
            targetPropName, targetPropValue);
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> getTypedRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, RelationshipType type) {
        String startNodeProps = buildConjunctiveConstraints(
            "s", startNode.jsonProperties);
        String endNodeProps = buildConjunctiveConstraints(
            "e", endNode.jsonProperties);
        String query = String.format(
            "MATCH (s:%s)-[r:%s]-(e:%s) WHERE %s AND %s RETURN r",
            startNode.getLabel(), type.name(), endNode.getLabel(),
            startNodeProps, endNodeProps);
        return postCypherQuery(query);
    }

    // play.libs.F.Function implementations

    private static class NodeURLFunction implements
                                             Function<WS.Response, String> {
        public String apply(WS.Response response) {
            JsonNode json = response.asJson();
            return json.findValue("self").asText();
        }
    }

    private static class NodesByURLFunction
        implements Function<List<String>, Promise<List<WS.Response>>> {
        public Promise<List<WS.Response>> apply(List<String> nodeURLs) {
            List<Promise<? extends WS.Response>> responses =
                new ArrayList<Promise<? extends WS.Response>>();
            for (String nodeURL: nodeURLs) {
                Promise<WS.Response> response = Neo4jService.getNodeByURL(
                    nodeURL);
                responses.add(response);
            }
            return Promise.sequence(responses);
        }
    }

    private static class TargetURLsFunction implements
        Function<WS.Response, List<String>> {
        public List<String> apply(WS.Response response) {
            JsonNode json = response.asJson();
            return json.findValuesAsText("end");
        }
    }

}
