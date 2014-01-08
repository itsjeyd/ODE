package models;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.mvc.Http.Status;

import neo4play.Neo4jService;


public class Relationship {
    private Neo4jService dbService = new Neo4jService();

    public String type;
    public Model startNode;
    public Model endNode;

    public Relationship(String type, Model startNode, Model endNode) {
        this.type = type;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Relationship> create() {
        return this.exists().flatMap(new GetOrCreateFunction(this));
    }

    public Promise<Boolean> exists() {
        Promise<WS.Response> response = dbService.getRelationship(this);
        return response.map(new ExistsFunction());
    }

    private class GetOrCreateFunction
        implements Function<Boolean, Promise<Relationship>> {
        private Relationship relationship;
        public GetOrCreateFunction(Relationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Relationship> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(this.relationship);
            } else {
                Promise<WS.Response> response = dbService.createRelationship(
                    this.relationship.startNode.label,
                    this.relationship.startNode.jsonProperties,
                    this.relationship.endNode.label,
                    this.relationship.endNode.jsonProperties,
                    this.relationship.type);
                return response.map(
                    new RelationshipCreatedFunction(this.relationship));
            }
        }
    }

    private class RelationshipCreatedFunction
        implements Function<WS.Response, Relationship> {
        private Relationship relationship;
        public RelationshipCreatedFunction(Relationship relationship) {
            this.relationship = relationship;
        }
        public Relationship apply(WS.Response response) {
            if (response.getStatus() == Status.CREATED) {
                return this.relationship;
            }
            return null;
        }
    }

    protected class ExistsFunction implements
                                       Function<WS.Response, Boolean> {
        public Boolean apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get("data").size() > 0) {
                return true;
            }
            return false;
        }
    }

}
