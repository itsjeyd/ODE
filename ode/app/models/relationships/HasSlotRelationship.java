package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.CombinationGroup;
import models.nodes.Slot;
import managers.relationships.HasSlotRelationshipManager;


public class HasSlotRelationship extends TypedRelationship {

    public HasSlotRelationship(CombinationGroup startNode,
                               Slot endNode) {
        super(RelationshipType.HAS, startNode, endNode);
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction());
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return HasSlotRelationshipManager
                .create(HasSlotRelationship.this);
        }
    }

}
