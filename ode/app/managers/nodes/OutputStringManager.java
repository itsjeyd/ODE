package managers.nodes;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.PropertyFunction;
import models.nodes.OutputString;


public class OutputStringManager extends LabeledNodeWithPropertiesManager {

    private static Promise<JsonNode> getIncomingRelationships(
        OutputString string) {
        Promise<WS.Response> response = Neo4jService
            .getIncomingRelationshipsByType(string.getLabel(),
                                            string.jsonProperties,
                                            RelationshipType.HAS.name());
        return response.map(new JsonFunction());
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

    public static Promise<Boolean> isOrphan(OutputString string) {
        Promise<JsonNode> relationships = OutputStringManager
            .getIncomingRelationships(string);
        return relationships.map(
            new Function<JsonNode, Boolean>() {
                public Boolean apply(JsonNode relationships) {
                    return relationships.size() == 0;
                }
            });
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
