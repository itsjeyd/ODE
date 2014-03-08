package models.relationships;

import models.nodes.AVM;
import models.nodes.Feature;


public class HasFeatureRelationship extends HasRelationship {
    public AVM startNode;
    public Feature endNode;

    public HasFeatureRelationship(AVM startNode, Feature endNode) {
        super(startNode, endNode, startNode.rule);
    }

}
