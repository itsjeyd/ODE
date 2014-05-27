package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.RHSRelationshipManager;
import models.nodes.Rule;
import models.nodes.RHS;


public class RHSRelationship extends TypedRelationship {

    public RHSRelationship(Rule startNode, RHS endNode) {
        super(RelationshipType.RHS, startNode, endNode);
    }

    public static Promise<Boolean> delete(
        Rule startNode, final RHS endNode) {
        return RHSRelationshipManager.delete(startNode, endNode);
    }

}
