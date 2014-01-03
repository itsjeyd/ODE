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
        String query = this.buildMatchQuery(label, props) + " RETURN n";
        ObjectNode content = Json.newObject();
        content.put("query", query);
        return this.post("/cypher", content);
    }

    public Promise<WS.Response> createLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = "CREATE (n:" + label + " { props }) RETURN n";
        ObjectNode content = Json.newObject();
        ObjectNode params = Json.newObject();
        params.put("props", props);
        content.put("query", query);
        content.put("params", params);
        return this.post("/cypher", content);
    }

    public Promise<WS.Response> deleteLabeledNodeWithProperties(
        String label, JsonNode props) {
        String query = this.buildMatchQuery(label, props) + " DELETE n";
        ObjectNode content = Json.newObject();
        content.put("query",  query);
        return this.post("/cypher", content);
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

    public Promise<WS.Response> createRelationship(
        String startNodeLabel, JsonNode startNodeProps,
        String endNodeLabel, JsonNode endNodeProps,
        final String relationshipType) {
        Promise<String> startNodeURL = this.getNodeURL(startNodeLabel,
                                                       startNodeProps);
        Promise<String> endNodeURL = this.getNodeURL(endNodeLabel,
                                                     endNodeProps);
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

    public Promise<String> getNodeProperty(
        String nodeURL, String propertyName) {
        String fullURL = nodeURL + "/properties";
        Promise<WS.Response> properties = WS.url(fullURL).get();
        return properties.map(new PropertyFunction(propertyName));
    }

    public Promise<String> getNodeURL(String label, JsonNode props) {
        Promise<WS.Response> nodeResponse = this
            .getLabeledNodeWithProperties(label, props);
        return nodeResponse.map(new NodeURLFunction());
    }

    private class NodeURLFunction implements Function<WS.Response, String> {
        public String apply(WS.Response response) {
            JsonNode json = response.asJson();
            return json.findValue("self").asText();
        }
    }

    private class PropertyFunction implements Function<WS.Response, String> {
        private String propertyName;
        public PropertyFunction(String propertyName) {
            this.propertyName = propertyName;
        }
        public String apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get(this.propertyName) != null) {
                return json.get(this.propertyName).asText();
            }
            return "";
        }
    }

}
