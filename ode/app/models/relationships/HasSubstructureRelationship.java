package models.relationships;

import play.libs.F.Promise;

import managers.relationships.HasSubstructureRelationshipManager;
import models.nodes.Feature;
import models.nodes.Substructure;


public class HasSubstructureRelationship extends HasRelationship {
    public Feature startNode;
    public Substructure endNode;

    public HasSubstructureRelationship(
        Feature startNode, Substructure endNode) {
        super(startNode, endNode, endNode.rule);
    }

}
