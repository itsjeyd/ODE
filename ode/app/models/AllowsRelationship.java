package models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.None;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Some;
import play.libs.F.Tuple;

import constants.RelationshipType;
import managers.AllowsRelationshipManager;
import managers.RelationshipManager;


public class AllowsRelationship extends TypedRelationship {

    public AllowsRelationship(Feature startNode, OntologyNode endNode) {
        this.type = RelationshipType.ALLOWS;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = AllowsRelationshipManager.get(this);
        return json.map(new ExistsFunction());
    }

    public Promise<Tuple<Option<Relationship>, Boolean>> getOrCreate() {
        return this.exists().flatMap(new GetOrCreateFunction(this));
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


    private class ExistsFunction implements Function<JsonNode, Boolean> {
        public Boolean apply(JsonNode json) {
            return json.get("data").size() > 0;
        }
    }

    private class GetOrCreateFunction
        implements Function<Boolean,
                   Promise<Tuple<Option<Relationship>, Boolean>>> {
        private AllowsRelationship relationship;
        public GetOrCreateFunction(AllowsRelationship relationship) {
            this.relationship = relationship;
        }
        public Promise<Tuple<Option<Relationship>, Boolean>> apply(
            Boolean exists) {
            if (exists) {
                return Promise.pure(
                    new Tuple<Option<Relationship>, Boolean>(
                        new Some<Relationship>(this.relationship), false));
            }
            Promise<Boolean> created = AllowsRelationshipManager.create(
                this.relationship);
            return created.map(new CreatedFunction(this.relationship));
        }
    }

    private class CreatedFunction
        implements Function<Boolean, Tuple<Option<Relationship>, Boolean>> {
        private Relationship relationship;
        public CreatedFunction(Relationship relationship) {
            this.relationship = relationship;
        }
        public Tuple<Option<Relationship>, Boolean> apply(Boolean created) {
            if (created) {
                return new Tuple<Option<Relationship>, Boolean>(
                    new Some<Relationship>(this.relationship), true);
            }
            return new Tuple<Option<Relationship>, Boolean>(
                new None<Relationship>(), false);
        }
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
