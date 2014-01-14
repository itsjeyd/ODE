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
import models.AllowsRelationship;
import models.Relationship;
import models.TypedRelationship;
import utils.StringUtils;


public class Neo4jService {
    public String rootURL;
    public String contentType;
    public String accept;

    public Neo4jService() {
        this.rootURL = "http://localhost:7474/db/data";
        this.contentType = "application/json";
        this.accept = "application/json; charset=UTF-8";
    }

    private String extendRootURL(String suffix) {
        if (suffix.startsWith("/")) {
            return this.rootURL + suffix;
        } else {
            return this.rootURL + "/" + suffix;
        }
    }

    protected String buildMatchQuery(String nodeLabel, JsonNode props) {
        return "MATCH (n:" + nodeLabel + ") WHERE "
            + this.buildConjunctiveConstraints(props);
    }

    protected String buildConjunctiveConstraints(JsonNode props) {
        List<String> constraints = new ArrayList<String>();
        for (Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
             fields.hasNext(); ) {
            constraints.add("n." + fields.next().toString());
        }
        return StringUtils.join(constraints, " AND ");
    }

    protected String buildConjunctiveConstraints(
        String varName, JsonNode props) {
        List<String> constraints = new ArrayList<String>();
        for (Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
             fields.hasNext(); ) {
            constraints.add(
                String.format("%s.%s", varName, fields.next().toString()));
        }
        return StringUtils.join(constraints, " AND ");
    }

    protected Promise<WS.Response> postCypherQuery(String query) {
        ObjectNode content = Json.newObject();
        content.put("query", query);
        return this.post("/cypher", content);
    }

    protected Promise<WS.Response> postCypherQueryWithParams(
        String query, JsonNode props) {
        ObjectNode params = Json.newObject();
        params.put("props", props);
        ObjectNode content = Json.newObject();
        content.put("query", query);
        content.put("params", params);
        return this.post("/cypher", content);
    }

    public Promise<WS.Response> get(String resourceURL) {
        String fullURL = this.extendRootURL(resourceURL);
        return WS.url(fullURL).get();
    }

    public Promise<WS.Response> post(String resourceURL, JsonNode content) {
        String fullURL = this.extendRootURL(resourceURL);
        return WS.url(fullURL).setContentType(this.contentType).
            post(content);
    }

    public Promise<WS.Response> delete(String url) {
        return WS.url(url).delete();
    }

    public Promise<WS.Response> getLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = this.buildMatchQuery(label, props) + " RETURN n";
        return this.postCypherQuery(query);
    }

    public Promise<WS.Response> createLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = "CREATE (n:" + label + " { props }) RETURN n";
        return this.postCypherQueryWithParams(query, props);
    }

    public Promise<WS.Response> deleteLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = this.buildMatchQuery(label, props) + " DELETE n";
        return this.postCypherQuery(query);
    }

    public Promise<WS.Response> getNodesByLabel(String label) {
        return this.get("/label/" + label + "/nodes");
    }

    public Promise<WS.Response> updateNodeProperties(
        String label, JsonNode oldProps, final JsonNode newProps) {
        Promise<String> nodeURL = this.getNodeURL(label, oldProps);
        return nodeURL.flatMap(new Function<String, Promise<WS.Response>>() {
            public Promise<WS.Response> apply(String nodeURL) {
                String fullURL = nodeURL + "/properties";
                return WS.url(fullURL).put(newProps);
            }});
    }

    public Promise<WS.Response> createTypedRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, final RelationshipType type) {
        Promise<String> startNodeURL = this.getNodeURL(
            startNode.label.toString(), startNode.jsonProperties);
        Promise<String> endNodeURL = this.getNodeURL(
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

    public Promise<WS.Response> getOutgoingRelationshipsByType(
        String startNodeLabel, JsonNode startNodeProps,
        final String relationshipType) {
        Promise<String> startNodeURL = this.getNodeURL(startNodeLabel,
                                                       startNodeProps);
        return startNodeURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String nodeURL) {
                    String fullURL = nodeURL + "/relationships/out/"
                        + relationshipType;
                    return WS.url(fullURL).get();
        }});
    }

    public Promise<WS.Response> getOutgoingRelationshipsByType(
        LabeledNodeWithProperties startNode, final RelationshipType type) {
        Promise<String> startNodeURL = this.getNodeURL(
            startNode.label.toString(), startNode.jsonProperties);
        return startNodeURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String nodeURL) {
                    String fullURL = nodeURL + "/relationships/out/"
                        + type.name();
                    return WS.url(fullURL).get();
        }});
    }

    public Promise<String> getNodeURL(String label, JsonNode props) {
        Promise<WS.Response> nodeResponse = this
            .getLabeledNodeWithProperties(label, props);
        return nodeResponse.map(new NodeURLFunction());
    }

    public Promise<WS.Response> getTypedRelationship(
        AllowsRelationship relationship) {
        String startNodeProps = buildConjunctiveConstraints(
            "s", relationship.startNode.jsonProperties);
        String endNodeProps = buildConjunctiveConstraints(
            "e", relationship.endNode.jsonProperties);
        String query = String.format(
            "MATCH (s:%s)-[r:%s]-(e:%s) WHERE %s AND %s RETURN r",
            relationship.startNode.label,
            relationship.type,
            relationship.endNode.label,
            startNodeProps,
            endNodeProps);
        return this.postCypherQuery(query);
    }

    public Promise<WS.Response> deleteRelationship(
        Relationship relationship) {
        String fullURL = this.extendRootURL(
            "/relationship/" + relationship.ID);
        return this.delete(fullURL);
    }

    public Promise<List<WS.Response>> getRelationshipTargets(
        String nodeLabel, JsonNode nodeProps, String relationshipType) {
        Promise<WS.Response> response = this
            .getOutgoingRelationshipsByType(
                nodeLabel, nodeProps, relationshipType);
        Promise<List<String>> targetURLs = response.map(
            new TargetURLsFunction());
        return targetURLs.flatMap(new NodesByURLFunction());
    }

    private class TargetURLsFunction implements
        Function<WS.Response, List<String>> {
        public List<String> apply(WS.Response response) {
            JsonNode json = response.asJson();
            return json.findValuesAsText("end");
        }
    }

    private class NodesByURLFunction
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

    public static Promise<WS.Response> getNodeByURL(String nodeURL) {
        return WS.url(nodeURL).get();
    }

    private class NodeURLFunction implements Function<WS.Response, String> {
        public String apply(WS.Response response) {
            JsonNode json = response.asJson();
            return json.findValue("self").asText();
        }
    }

}
