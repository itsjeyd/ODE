package managers.nodes;

import java.util.UUID;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.NodeDeletedFunction;
import models.nodes.AVM;


public class AVMManager extends LabeledNodeWithPropertiesManager {

    public static Promise<Boolean> delete(final AVM avm) {
        Promise<UUID> uuid = avm.getUUID();
        Promise<WS.Response> response = uuid.flatMap(
            new Function<UUID, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(UUID uuid) {
                    avm.jsonProperties.put("uuid", uuid.toString());
                    return Neo4jService.deleteLabeledNodeWithProperties(
                        avm.getLabel(), avm.jsonProperties);
                }
            });
        return response.map(new NodeDeletedFunction());
    }

}
