package managers.relationships;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import constants.RelationshipType;
import neo4play.Neo4jService;
import models.nodes.CombinationGroup;
import models.nodes.RHS;


public class GroupRelationshipManager extends TypedRelationshipManager {

    public static Promise<Boolean> delete(
        final RHS startNode, final CombinationGroup endNode) {
        Promise<WS.Response> response = Neo4jService.deleteTypedRelationship(
            startNode, endNode, RelationshipType.HAS);
        return response.map(
            new Function<WS.Response, Boolean>() {
                public Boolean apply(WS.Response response) {
                    return response.getStatus() == Status.OK;
                }
            });
    }

}
