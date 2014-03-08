package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.HasSubstructureRelationshipManager;
import models.nodes.Feature;
import models.nodes.Substructure;


public class HasSubstructureRelationship extends TypedRelationship {
    public Feature startNode;
    public Substructure endNode;

    private HasSubstructureRelationship() {
        this.type = RelationshipType.HAS;
    }

    public HasSubstructureRelationship(
        Feature startNode, Substructure endNode) {
        this();
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> create() {
        return Promise.pure(false).flatMap(new CreateFunction(this));
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private HasSubstructureRelationship relationship;
        public CreateFunction(HasSubstructureRelationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return HasSubstructureRelationshipManager
                .create(this.relationship);
        }
    }

}
