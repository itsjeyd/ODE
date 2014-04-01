package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.LHSRelationshipManager;
import models.nodes.LHS;
import models.nodes.Rule;


public class LHSRelationship extends TypedRelationship {

    private LHSRelationship() {
        this.type = RelationshipType.LHS;
    }

    public LHSRelationship(Rule startNode, LHS endNode) {
        this();
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    public static Promise<Boolean> delete(
        Rule startNode, final LHS endNode) {
        return LHSRelationshipManager.delete(startNode, endNode);
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
