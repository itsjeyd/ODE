package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
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

}
