package models.relationships;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.AllowsRelationshipManager;
import managers.relationships.RelationshipManager;
import models.nodes.Feature;
import models.nodes.OntologyNode;


public class AllowsRelationship extends TypedRelationship {

    private AllowsRelationship() {
        this.type = RelationshipType.ALLOWS;
    }

    public AllowsRelationship(Feature startNode, OntologyNode endNode) {
        this();
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private AllowsRelationship relationship;
        public CreateFunction(AllowsRelationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return AllowsRelationshipManager.create(this.relationship);
        }
    }

    @Override
    public Promise<Boolean> delete() {
        Promise<JsonNode> json = AllowsRelationshipManager.get(this);
        Promise<Relationship> relationship = json.map(new GetFunction(this));
        return relationship.flatMap(
            new Function<Relationship, Promise<Boolean>>() {
                public Promise<Boolean> apply(Relationship relationship) {
                    return RelationshipManager.delete(relationship);
                }
            });
    }

    public static Promise<Boolean> deleteAllFrom(Feature startNode) {
        Promise<List<Relationship>> outgoingRelationships = startNode
            .getOutgoingRelationships(RelationshipType.ALLOWS);
        Promise<List<Boolean>> deletedList = outgoingRelationships.flatMap(
            new Function<List<Relationship>, Promise<List<Boolean>>>() {
                public Promise<List<Boolean>> apply(
                    List<Relationship> relationships) {
                    List<Promise<? extends Boolean>> deletedList =
                        new ArrayList<Promise<? extends Boolean>>();
                    for (Relationship relationship: relationships) {
                        Promise<Boolean> deleted = relationship.delete();
                        deletedList.add(deleted);
                    }
                    return Promise.sequence(deletedList);
                }
            });
        return deletedList.map(
            new Function<List<Boolean>, Boolean>() {
                public Boolean apply(List<Boolean> deletedList) {
                    Boolean allDeleted = true;
                    for (Boolean deleted: deletedList) {
                        if (!deleted) {
                            allDeleted = false;
                            break;
                        }
                    }
                    return allDeleted;
                }
            });
    }

    private class GetFunction implements Function<JsonNode, Relationship> {
        private Relationship relationship;
        public GetFunction(Relationship relationship) {
            this.relationship = relationship;
        }
        public Relationship apply(JsonNode json) {
            String url = json.findValue("self").asText();
            int ID = Integer.parseInt(
                url.substring(url.lastIndexOf("/") + 1));
            this.relationship.ID = ID;
            return this.relationship;
        }
    }

}
