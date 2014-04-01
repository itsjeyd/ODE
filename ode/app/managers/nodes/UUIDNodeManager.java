package managers.nodes;

import java.util.UUID;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.NodeDeletedFunction;
import models.nodes.UUIDNode;


public class UUIDNodeManager extends LabeledNodeWithPropertiesManager {

    public static Promise<Boolean> delete(final UUIDNode uuidNode) {
        Promise<UUID> uuid = uuidNode.getUUID();
        Promise<WS.Response> response = uuid.flatMap(
            new Function<UUID, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(UUID uuid) {
                    uuidNode.jsonProperties.put("uuid", uuid.toString());
                    return Neo4jService.deleteLabeledNodeWithProperties(
                        uuidNode.getLabel(), uuidNode.jsonProperties);
                }
            });
        return response.map(new NodeDeletedFunction());
    }

}
