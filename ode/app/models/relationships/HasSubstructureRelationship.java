package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.HasSubstructureRelationshipManager;
import models.nodes.AVM;
import models.nodes.Feature;


public class HasSubstructureRelationship extends TypedRelationship {
    public Feature startNode;
    public AVM endNode;

    public HasSubstructureRelationship(AVM avm) {
        this.type = RelationshipType.HAS;
        this.startNode = avm.embeddingFeature;
        this.endNode = avm;
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
