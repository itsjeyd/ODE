package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.RHSRelationshipManager;
import models.functions.ExistsFunction;
import models.nodes.Rule;
import models.nodes.RHS;


public class RHSRelationship extends TypedRelationship {

    private RHSRelationship() {
        this.type = RelationshipType.RHS;
    }

    public RHSRelationship(Rule startNode, RHS endNode) {
        this();
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    public static Promise<Boolean> delete(
        Rule startNode, final RHS endNode) {
        return RHSRelationshipManager.delete(startNode, endNode);
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private RHSRelationship relationship;
        public CreateFunction(RHSRelationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return RHSRelationshipManager.create(this.relationship);
        }
    }

}
