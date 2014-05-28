package models.relationships;

import constants.RelationshipType;
import models.nodes.LHS;
import models.nodes.Rule;


public class LHSRelationship extends TypedRelationship {

    public LHSRelationship(Rule startNode, LHS endNode) {
        super(RelationshipType.LHS, startNode, endNode);
    }

}
