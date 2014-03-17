package managers.relationships;

import java.util.UUID;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import constants.RelationshipType;
import neo4play.Neo4jService;
import models.nodes.RHS;
import models.nodes.Rule;


public class RHSRelationshipManager extends TypedRelationshipManager {

    public static Promise<Boolean> delete(
        final Rule startNode, final RHS endNode) {
        Promise<UUID> uuid = endNode.getUUID();
        Promise<WS.Response> response = uuid.flatMap(
            new Function<UUID, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(UUID uuid) {
                    endNode.jsonProperties.put("uuid", uuid.toString());
                    return Neo4jService.deleteTypedRelationship(
                        startNode, endNode, RelationshipType.RHS);
                }
            });
        return response.map(
            new Function<WS.Response, Boolean>() {
                public Boolean apply(WS.Response response) {
                    return response.getStatus() == Status.OK;
                }
            });
    }

}
