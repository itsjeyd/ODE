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

    public Promise<WS.Response> get(String resourceURL) {
        String fullURL = this.extendRootURL(resourceURL);
        return WS.url(fullURL).get();
    }

    public Promise<WS.Response> post(String resourceURL, JsonNode content) {
        String fullURL = this.extendRootURL(resourceURL);
        return WS.url(fullURL).setContentType(this.contentType).
            post(content);
    }

    public Promise<WS.Response> getLabeledNodeWithProperties(
        String label, JsonNode props) {
        String fullURL = this.extendRootURL("/cypher");
        String query = "MATCH (n:" + label + ") WHERE ";
        List<String> constraints = new ArrayList<String>();
        for (Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
             fields.hasNext(); ) {
            constraints.add("n." + fields.next().toString());
        }
        query += StringUtils.join(constraints, " AND ");
        query += " RETURN n";
        ObjectNode content = Json.newObject();
        content.put("query", query);
        return WS.url(fullURL).setContentType(this.contentType)
            .post(content);
    }

    public Promise<WS.Response> createLabeledNodeWithProperties(
        String label, JsonNode props) {
        String fullURL = this.extendRootURL("/cypher");
        String query = "CREATE (n:" + label + " { props }) RETURN n";
        ObjectNode content = Json.newObject();
        ObjectNode params = Json.newObject();
        params.put("props", props);
        content.put("query", query);
        content.put("params", params);
        return WS.url(fullURL).setContentType(this.contentType)
            .post(content);
    }

    public Promise<WS.Response> deleteLabeledNodeWithProperties(
        String label, JsonNode props) {
        String fullURL = this.extendRootURL("/cypher");
        String query = "MATCH (n:" + label + ") WHERE ";
        List<String> constraints = new ArrayList<String>();
        for (Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
             fields.hasNext(); ) {
            constraints.add("n." + fields.next().toString());
        }
        query += StringUtils.join(constraints, " AND ");
        query += " DELETE n";
        ObjectNode content = Json.newObject();
        content.put("query",  query);
        return WS.url(fullURL).setContentType(this.contentType)
            .post(content);
    }

    public Promise<WS.Response> getNodesByLabel(String label) {
        String fullURL = this.extendRootURL("/label/" + label + "/nodes");
        return WS.url(fullURL).get();
    }

    public Promise<WS.Response> updateNodeProperties(
        String label, JsonNode oldProps, final JsonNode newProps) {
        Promise<WS.Response> response = this.getLabeledNodeWithProperties(
            label, oldProps);
        Promise<String> nodeURL = response.map(new NodeURLFunction());
        return nodeURL.flatMap(new Function<String, Promise<WS.Response>>() {
            public Promise<WS.Response> apply(String nodeURL) {
                String fullURL = nodeURL + "/properties";
                return WS.url(fullURL).put(newProps);
            }});
    }

    public Promise<WS.Response> createRelationship(
        String startNodeLabel, JsonNode startNodeProps,
        String endNodeLabel, JsonNode endNodeProps,
        final String relationshipType) {
        Promise<WS.Response> startNodeResponse = this
            .getLabeledNodeWithProperties(startNodeLabel, startNodeProps);
        Promise<String> startNodeURL = startNodeResponse.map(
            new NodeURLFunction());
        Promise<WS.Response> endNodeResponse = this
            .getLabeledNodeWithProperties(endNodeLabel, endNodeProps);
        Promise<String> endNodeURL = endNodeResponse.map(
            new NodeURLFunction());
        Promise<Tuple<String, String>> urls = startNodeURL.zip(
            endNodeURL);
        return urls.flatMap(
            new Function<Tuple<String, String>, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(
                    Tuple<String, String> urls) {
                    String fullURL = urls._1 + "/relationships";
                    ObjectNode content = Json.newObject();
                    content.put("to", urls._2);
                    content.put("type", relationshipType);
                    return WS.url(fullURL).post(content);
                }
            });
    }

    private class NodeURLFunction implements Function<WS.Response, String> {
        public String apply(WS.Response response) {
            JsonNode json = response.asJson();
            return json.findValue("self").asText();
        }
    }

}
