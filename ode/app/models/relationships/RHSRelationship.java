package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.RHSRelationshipManager;
import models.nodes.Rule;
import models.nodes.RHS;


public class RHSRelationship extends TypedRelationship {

    public RHSRelationship(Rule startNode, RHS endNode) {
        super(RelationshipType.RHS, startNode, endNode);
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
