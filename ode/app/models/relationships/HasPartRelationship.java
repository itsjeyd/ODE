package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.Part;
import models.nodes.Slot;
import managers.relationships.HasPartRelationshipManager;


public class HasPartRelationship extends TypedRelationship {

    public HasPartRelationship(Slot startNode, Part endNode) {
        super(RelationshipType.HAS, startNode, endNode);
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction());
    }

    public static Promise<Boolean> delete(Slot startNode, Part endNode) {
        return HasPartRelationshipManager.delete(startNode, endNode);
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return HasPartRelationshipManager
                .create(HasPartRelationship.this);
        }
    }

}
