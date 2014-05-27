package models.relationships;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.LHSRelationshipManager;
import models.nodes.LHS;
import models.nodes.Rule;


public class LHSRelationship extends TypedRelationship {

    public LHSRelationship(Rule startNode, LHS endNode) {
        super(RelationshipType.LHS, startNode, endNode);
    }

    public static Promise<Boolean> delete(
        Rule startNode, final LHS endNode) {
        return LHSRelationshipManager.delete(startNode, endNode);
    }

}
