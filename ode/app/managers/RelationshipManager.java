package managers;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.DeletedFunction;
import models.Relationship;
import neo4play.Neo4jService;


public class RelationshipManager {

    public static Promise<Boolean> delete(Relationship relationship) {
        Promise<WS.Response> response = Neo4jService.deleteRelationship(
            relationship);
        return response.map(new DeletedFunction());
    }

}
