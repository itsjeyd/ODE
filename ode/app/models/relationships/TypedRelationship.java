package models.relationships;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.TypedRelationshipManager;
import models.functions.ExistsFunction;
import models.nodes.Feature;


public class TypedRelationship extends Relationship {
    public RelationshipType type;

    public TypedRelationship() {
        super();
    }

    public TypedRelationship(int ID) {
        this.ID = ID;
    }

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = TypedRelationshipManager.get(this);
        return json.map(new ExistsFunction());
    }

    public static Promise<List<Relationship>> getAllFrom(
        Feature startNode, RelationshipType type) {
        Promise<JsonNode> json = TypedRelationshipManager.getAllFrom(
            startNode, type);
        return json.map(new AllFromFunction());
    }

    private static class AllFromFunction
        implements Function<JsonNode, List<Relationship>> {
        public List<Relationship> apply(JsonNode json) {
            List<Relationship> relationships = new ArrayList<Relationship>();
            Iterator<JsonNode> relationshipIterator = json.elements();
            while (relationshipIterator.hasNext()) {
                JsonNode relationship = relationshipIterator.next();
                String url = relationship.get("self").asText();
                int ID = Integer.parseInt(
                    url.substring(url.lastIndexOf("/") + 1));
                relationships.add(new TypedRelationship(ID));
            }
            return relationships;
        }
    }

}
