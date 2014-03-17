package managers.nodes;

import java.util.UUID;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.NodeDeletedFunction;
import models.nodes.RHS;


public class RHSManager extends LabeledNodeWithPropertiesManager {

    public static Promise<Boolean> delete(final RHS rhs) {
        Promise<UUID> uuid = rhs.getUUID();
        Promise<WS.Response> response = uuid.flatMap(
            new Function<UUID, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(UUID uuid) {
                    rhs.jsonProperties.put("uuid", uuid.toString());
                    return Neo4jService.deleteLabeledNodeWithProperties(
                        rhs.getLabel(), rhs.jsonProperties);
                }
            });
        return response.map(new NodeDeletedFunction());
    }

}
