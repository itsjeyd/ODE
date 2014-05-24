package managers.relationships;

import play.libs.WS;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.RelationshipDeletedFunction;
import models.relationships.Relationship;


public class RelationshipManager {

    public static Promise<Boolean> delete(Relationship relationship) {
        Promise<WS.Response> response = Neo4jService.deleteRelationship(
            relationship);
        return response.map(new RelationshipDeletedFunction());
    }

}
