package managers.nodes;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.PropertyFunction;
import models.nodes.Part;


public class PartManager extends LabeledNodeWithPropertiesManager {

    public PartManager() {
        this.label = "Part";
    }

    public Promise<List<Part>> all() {
        Promise<List<JsonNode>> json = all(this.label);
        return json.map(
            new Function<List<JsonNode>, List<Part>>() {
                public List<Part> apply(List<JsonNode> json) {
                    List<Part> parts = new ArrayList<Part>();
                    for (JsonNode node: json) {
                        String content = node.get("content").asText();
                        parts.add(new Part(content));
                    }
                    return parts;
                }
            });
    }

    @Override
    public Promise<Boolean> create(
        final JsonNode properties, final String location) {
        ObjectNode props = (ObjectNode) properties.deepCopy();
        Promise<Boolean> exists = exists(props.retain("uuid"));
        Promise<Boolean> created = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return Promise.pure(true);
                    }
                    return PartManager.super.create(properties, location);
                }
            });
        return created;
    }


    private static Promise<String> getProperty(Part part, String propName) {
        Promise<String> partURL = Neo4jService
            .getNodeURL(part.getLabel(), part.jsonProperties);
        Promise<WS.Response> response = partURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String partURL) {
                    return Neo4jService.getNodeProperties(partURL);
                }
            });
        Promise<JsonNode> json = response.map(new JsonFunction());
        return json.map(new PropertyFunction(propName));
    }

    public static Promise<UUID> getUUID(Part part) {
        Promise<String> prop = getProperty(part, "uuid");
        return prop.map(
            new Function<String, UUID>() {
                public UUID apply(String prop) {
                    return UUID.fromString(prop);
                }
            });
    }

}
