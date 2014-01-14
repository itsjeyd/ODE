package models;

import play.libs.WS;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.DeletedFunction;


public abstract class Relationship extends Model {
    public int ID;
    public LabeledNodeWithProperties startNode;
    public LabeledNodeWithProperties endNode;

    public Promise<Boolean> delete() {
        return Relationship.Manager.delete(this);
    };


    public static class Manager {
        private static Neo4jService dbService = new Neo4jService();
        public static Promise<Boolean> delete(Relationship relationship) {
            Promise<WS.Response> response = dbService.deleteRelationship(
                relationship);
            return response.map(new DeletedFunction());
        }
    }
}
