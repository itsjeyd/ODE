package managers.nodes;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.PropertyFunction;
import models.nodes.OutputString;


public class OutputStringManager extends LabeledNodeWithPropertiesManager {

    public OutputStringManager() {
        this.label = "OutputString";
    }

    @Override
    public Promise<Boolean> create(
        final JsonNode properties, final String location) {
        final ObjectNode props = (ObjectNode) properties.deepCopy();
        Promise<Boolean> exists = exists(props.retain("uuid"));
        Promise<Boolean> created = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return Promise.pure(true);
                    }
                    return OutputStringManager.super
                        .create(properties, location);
                }
            });
        return created;
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
