package models.relationships;

import constants.RelationshipType;
import models.nodes.Rule;
import models.nodes.RHS;


public class RHSRelationship extends TypedRelationship {

    public RHSRelationship(Rule startNode, RHS endNode) {
        super(RelationshipType.RHS, startNode, endNode);
    }

}
