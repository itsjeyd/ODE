package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.Rule;
import models.nodes.Slot;
import managers.relationships.HasRefRelationshipManager;


public class HasRefRelationship extends TypedRelationship {

    public HasRefRelationship(Slot startNode, Rule endNode) {
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
            return HasRefRelationshipManager
                .create(HasRefRelationship.this);
        }
    }

}
