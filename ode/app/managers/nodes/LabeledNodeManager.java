package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import managers.functions.SuccessFunction;
import managers.relationships.RelManager;
import models.nodes.LabeledNodeWithProperties;
import neo4play.NodeService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public abstract class LabeledNodeManager extends NodeManager {

    protected String label;

    public Promise<Boolean> exists(JsonNode properties) {
        return exists(this.label, properties);
    }

    protected Promise<Boolean> create(JsonNode properties, String location) {
        Promise<WS.Response> response =
            NodeService.createNode(this.label, properties, location);
        return response.map(new SuccessFunction());
    }

    protected Promise<Boolean> create(
        final JsonNode properties, final String location, String idField) {
        final ObjectNode props = (ObjectNode) properties.deepCopy();
        Promise<Boolean> exists = exists(props.retain(idField));
        Promise<Boolean> created = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return Promise.pure(false);
                    }
                    Promise<WS.Response> response = NodeService.createNode(
                        LabeledNodeManager.this.label, properties, location);
                    return response.map(new SuccessFunction());
                }
            });
        return created;
    }

    public Promise<Boolean> update(
        JsonNode oldProperties, JsonNode newProperties) {
        return update(this.label, oldProperties, newProperties);
    }

    protected Promise<Boolean> delete(JsonNode properties, String location) {
        Promise<WS.Response> response =
            NodeService.deleteNode(this.label, properties, location);
        return response.map(new SuccessFunction());
    }

    protected Promise<Boolean> orphaned(
        LabeledNodeWithProperties node, RelManager relManager) {
        Promise<List<JsonNode>> incomingRelationships =
            relManager.to(node);
        return incomingRelationships.map(
            new Function<List<JsonNode>, Boolean>() {
                public Boolean apply(List<JsonNode> incomingRelationships) {
                    return incomingRelationships.size() == 0;
                }
            });
    }

}
