package neo4play;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;


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
}
