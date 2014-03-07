package models.relationships;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import managers.RelationshipManager;
import models.Model;
import models.nodes.LabeledNodeWithProperties;


public abstract class Relationship extends Model {
    public int ID;
    public LabeledNodeWithProperties startNode;
    public LabeledNodeWithProperties endNode;

    public Relationship() {
        this.ID = -1;
    }

    public static Promise<List<Relationship>> getAllTo(
        LabeledNodeWithProperties endNode) {
        Promise<JsonNode> json = RelationshipManager.getAllTo(endNode);
        return json.map(new AllToFunction());
    }

    public Promise<Boolean> delete() {
        return RelationshipManager.delete(this);
    };

    private static class AllToFunction
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
