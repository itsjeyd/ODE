package managers.nodes;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.PropertyFunction;
import models.nodes.Part;


public class PartManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> staticAll() {
        return LabeledNodeManager.all(NodeType.PART);
    }

    private static Promise<JsonNode> getIncomingRelationships(Part part) {
        Promise<WS.Response> response = Neo4jService
            .getIncomingRelationshipsByType(part.getLabel(),
                                            part.jsonProperties,
                                            RelationshipType.HAS.name());
        return response.map(new JsonFunction());
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

    public static Promise<Boolean> isOrphan(Part part) {
        Promise<JsonNode> relationships = PartManager
            .getIncomingRelationships(part);
        return relationships.map(
            new Function<JsonNode, Boolean>() {
                public Boolean apply(JsonNode relationships) {
                    return relationships.size() == 0;
                }
            });
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
