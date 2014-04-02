package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.Feature;


public class AllowsRelationshipManager extends TypedRelationshipManager {

    public static Promise<JsonNode> getAllFrom(Feature startNode) {
        return TypedRelationshipManager.getAllFrom(startNode,
                                                   RelationshipType.ALLOWS);
    }

}
