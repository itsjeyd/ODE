package models;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.LHSRelationshipManager;


public class LHSRelationship extends TypedRelationship {

    public LHSRelationship(LHS lhs) {
        this.type = RelationshipType.LHS;
        this.startNode = lhs.rule;
        this.endNode = lhs;
    }

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = LHSRelationshipManager.get(this);
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
        private LHSRelationship relationship;
        public CreateFunction(LHSRelationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return LHSRelationshipManager.create(this.relationship);
        }
    }

}
