package managers.relationships;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import models.relationships.HasRelationship;
import managers.functions.RelationshipCreatedFunction;


public class HasRelationshipManager {

    public static Promise<JsonNode> get(HasRelationship relationship) {
        return null;
    }

}
