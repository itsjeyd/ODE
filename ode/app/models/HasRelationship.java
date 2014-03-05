package models;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.HasRelationshipManager;


public class HasRelationship extends TypedRelationship {

    public HasRelationship(LHS startNode, Feature endNode) {
        super(-1);
        this.type = RelationshipType.HAS;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = HasRelationshipManager.get(this);
        return json.map(new ExistsFunction());
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    private class ExistsFunction implements Function<JsonNode, Boolean> {
        public Boolean apply(JsonNode json) {
            return json.get("data").size() > 0;
        }
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private HasRelationship relationship;
        public CreateFunction(HasRelationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return HasRelationshipManager.create(this.relationship);
        }
    }

}
