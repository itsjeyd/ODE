package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.GroupRelationshipManager;
import models.nodes.CombinationGroup;
import models.nodes.RHS;


public class GroupRelationship extends TypedRelationship {

    public GroupRelationship() {
        this.type = RelationshipType.HAS;
    }

    public GroupRelationship(RHS startNode, CombinationGroup endNode) {
        this();
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private GroupRelationship relationship;
        public CreateFunction(GroupRelationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return GroupRelationshipManager.create(this.relationship);
        }
    }
}
