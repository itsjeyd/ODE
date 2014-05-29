package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import managers.functions.JsonFunction;
import managers.functions.PropertyFunction;
import models.nodes.OutputString;
import neo4play.Neo4jService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public class OutputStringManager extends LabeledNodeWithPropertiesManager {

    public OutputStringManager() {
        this.label = "OutputString";
    }

    // READ

    @Override
    public Promise<Boolean> exists(JsonNode properties) {
        return super.exists(properties, "uuid");
    }

    // CREATE

    @Override
    public Promise<Boolean> create(
        final JsonNode properties, final String location) {
        Promise<Boolean> exists = exists(properties);
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
