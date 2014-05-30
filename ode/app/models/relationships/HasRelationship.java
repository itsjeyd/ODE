package models.relationships;

import constants.RelationshipType;
import models.nodes.LabeledNodeWithProperties;
import models.nodes.Rule;


public class HasRelationship extends TypedRelationship {
    public Rule rule;

    protected HasRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode,
        Rule rule) {
        super(RelationshipType.HAS, startNode, endNode);
        this.rule = rule;
    }

}
