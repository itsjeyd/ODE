package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.CombinationGroup;
import models.nodes.OutputString;
import managers.relationships.HasStringRelationshipManager;


public class HasStringRelationship extends TypedRelationship {

    public HasStringRelationship(CombinationGroup startNode,
                                 OutputString endNode) {
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
            return HasStringRelationshipManager
                .create(HasStringRelationship.this);
        }
    }

}
