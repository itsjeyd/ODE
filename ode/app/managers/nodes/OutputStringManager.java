package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.UUID;
import managers.functions.JsonFunction;
import managers.functions.PropertyFunction;
import models.nodes.OutputString;
import neo4play.Neo4jService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public class OutputStringManager extends ContentNodeManager {

    public OutputStringManager() {
        this.label = "OutputString";
    }

    // Custom functionality

    protected JsonNode toJSON(JsonNode properties) {
        ArrayNode tokens = JsonNodeFactory.instance.arrayNode();
        String content = properties.get("content").asText();
        String[] contentTokens = content.split(" ");
        for (String token: contentTokens) {
            tokens.add(token);
        }
        ((ObjectNode) properties).put("tokens", tokens);
        return properties;
    }


    private static Promise<String> getProperty(OutputString string,
                                               String propName) {
        Promise<String> stringURL = Neo4jService
            .getNodeURL(string.getLabel(), string.jsonProperties);
        Promise<WS.Response> response = stringURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String stringURL) {
                    return Neo4jService.getNodeProperties(stringURL);
                }
            });
        Promise<JsonNode> json = response.map(new JsonFunction());
        return json.map(new PropertyFunction(propName));
    }

    public static Promise<UUID> getUUID(OutputString string) {
        Promise<String> prop = getProperty(string, "uuid");
        return prop.map(
            new Function<String, UUID>() {
                public UUID apply(String prop) {
                    return UUID.fromString(prop);
                }
            });
    }

}
