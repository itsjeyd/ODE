package models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.None;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Some;
import play.libs.F.Tuple;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.RelationshipCreatedFunction;


public class AllowsRelationship extends TypedRelationship {
    public Feature startNode;
    public OntologyNode endNode;

    public AllowsRelationship(Feature startNode, OntologyNode endNode) {
        super(-1); // Set to "invalid" ID by default
        this.type = RelationshipType.ALLOWS;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = AllowsRelationship.Manager.get(this);
        return json.map(new ExistsFunction());
    }

    public Promise<Tuple<Option<Relationship>, Boolean>> getOrCreate() {
        return this.exists().flatMap(new GetOrCreateFunction(this));
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
            Promise<Boolean> created = AllowsRelationship.Manager.create(
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

    public static class Manager {
        private static Neo4jService dbService = new Neo4jService();
        public static Promise<JsonNode> get(AllowsRelationship relationship) {
            Promise<WS.Response> response = dbService.getTypedRelationship(
                relationship);
            return response.map(new JsonFunction());
        }
        public static Promise<Boolean> create(
            AllowsRelationship relationship) {
            Promise<WS.Response> response = dbService
                .createTypedRelationship(
                    relationship.startNode, relationship.endNode,
                    relationship.type);
            return response.map(new RelationshipCreatedFunction());
        }
    }

}
