package neo4play;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import constants.RelationshipType;
import models.LabeledNodeWithProperties;
import models.Relationship;
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

    protected static Promise<String> getNodeURL(
        String label, JsonNode props) {
        Promise<WS.Response> nodeResponse = getLabeledNodeWithProperties(
            label, props);
        return nodeResponse.map(new NodeURLFunction());
    }

    protected static Promise<WS.Response> getNodeByURL(String nodeURL) {
        return WS.url(nodeURL).get();
    }

    // API for external clients (managers)

    public static Promise<WS.Response> getLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = buildNodeQuery(label, props) + " RETURN n";
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> createLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = "CREATE (n:" + label + " { props }) RETURN n";
        return postCypherQueryWithParams(query, props);
    }

    public static Promise<WS.Response> deleteLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = buildNodeQuery(label, props) + " DELETE n";
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> getNodesByLabel(String label) {
        return get("/label/" + label + "/nodes");
    }

    public static Promise<WS.Response> getNodeProperties(String nodeURL) {
        String fullURL = nodeURL + "/properties";
        return WS.url(fullURL).get();
    }

    public static Promise<WS.Response> updateNodeProperties(
        String label, JsonNode oldProps, final JsonNode newProps) {
        Promise<String> nodeURL = getNodeURL(label, oldProps);
        return nodeURL.flatMap(new Function<String, Promise<WS.Response>>() {
            public Promise<WS.Response> apply(String nodeURL) {
                String fullURL = nodeURL + "/properties";
                return WS.url(fullURL).put(newProps);
            }});
    }

    public static Promise<WS.Response> getIncomingRelationships(
        String endNodeLabel, JsonNode endNodeProps) {
        Promise<String> endNodeURL = getNodeURL(endNodeLabel, endNodeProps);
        return endNodeURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String nodeURL) {
                    String fullURL = nodeURL + "/relationships/in";
                    return WS.url(fullURL).get();
        }});
    }

    public static Promise<WS.Response> getIncomingRelationshipsByType(
        String endNodeLabel, JsonNode endNodeProps,
        final String relationshipType) {
        Promise<String> endNodeURL = getNodeURL(endNodeLabel, endNodeProps);
        return endNodeURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String nodeURL) {
                    String fullURL = nodeURL + "/relationships/in/"
                        + relationshipType;
                    return WS.url(fullURL).get();
        }});
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

    public static Promise<List<WS.Response>> getRelationshipTargets(
        String nodeLabel, JsonNode nodeProps, String relationshipType) {
        Promise<WS.Response> response = getOutgoingRelationshipsByType(
            nodeLabel, nodeProps, relationshipType);
        Promise<List<String>> targetURLs = response.map(
            new TargetURLsFunction());
        return targetURLs.flatMap(new NodesByURLFunction());
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
            startNode.label.toString(), type.name(), endNode.label.toString(),
            startNodeProps, endNodeProps);
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> createTypedRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, final RelationshipType type) {
        Promise<String> startNodeURL = getNodeURL(
            startNode.label.toString(), startNode.jsonProperties);
        Promise<String> endNodeURL = getNodeURL(
            endNode.label.toString(), endNode.jsonProperties);
        Promise<Tuple<String, String>> urls = startNodeURL.zip(
            endNodeURL);
        return urls.flatMap(
            new Function<Tuple<String, String>, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(
                    Tuple<String, String> urls) {
                    String fullURL = urls._1 + "/relationships";
                    ObjectNode content = Json.newObject();
                    content.put("to", urls._2);
                    content.put("type", type.name());
                    return WS.url(fullURL).post(content);
                }
            });
    }

    public static Promise<WS.Response> createTypedRelationshipWithProperties(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, final RelationshipType type,
        final JsonNode data) {
        Promise<String> startNodeURL = getNodeURL(
            startNode.label.toString(), startNode.jsonProperties);
        Promise<String> endNodeURL = getNodeURL(
            endNode.label.toString(), endNode.jsonProperties);
        Promise<Tuple<String, String>> urls = startNodeURL.zip(
            endNodeURL);
        return urls.flatMap(
            new Function<Tuple<String, String>, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(
                    Tuple<String, String> urls) {
                    String fullURL = urls._1 + "/relationships";
                    ObjectNode content = Json.newObject();
                    content.put("to", urls._2);
                    content.put("type", type.name());
                    content.put("data", data);
                    return WS.url(fullURL).post(content);
                }
            });
    }

    public static Promise<WS.Response> deleteRelationship(
        Relationship relationship) {
        String fullURL = extendRootURL("/relationship/" + relationship.ID);
        return delete(fullURL);
    }

    public static Promise<WS.Response> getPath(
        RelationshipType type, JsonNode relProps) {
        String query = "MATCH p=()-[r:" + type.name() + "]->() WHERE " +
            buildConjunctiveConstraints("r", relProps) + " RETURN p";
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
