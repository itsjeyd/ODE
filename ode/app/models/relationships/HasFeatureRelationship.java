package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.HasFeatureRelationshipManager;
import models.nodes.Feature;
import models.nodes.LHS;


public class HasFeatureRelationship extends TypedRelationship {
    public LHS startNode;
    public Feature endNode;

    public HasFeatureRelationship(LHS startNode, Feature endNode) {
        this.type = RelationshipType.HAS;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = HasFeatureRelationshipManager.get(this);
        return json.map(new ExistsFunction());
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    private class ExistsFunction implements Function<JsonNode, Boolean> {
        public Boolean apply(JsonNode json) {
            return json.get("data").size() > 0;
        }
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private HasFeatureRelationship relationship;
        public CreateFunction(HasFeatureRelationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return HasFeatureRelationshipManager.create(this.relationship);
        }
    }

}
