package managers;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.DeletedFunction;
import models.Relationship;
import neo4play.Neo4jService;


public class RelationshipManager {

    private static Neo4jService dbService = new Neo4jService();

    public static Promise<Boolean> delete(Relationship relationship) {
        Promise<WS.Response> response = dbService.deleteRelationship(
            relationship);
        return response.map(new DeletedFunction());
    }

}
