package neo4play;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Promise;

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
}
