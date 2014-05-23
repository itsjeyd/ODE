package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import managers.BaseManager;
import managers.functions.JsonFunction;
import managers.functions.SuccessFunction;
import models.nodes.Node;
import neo4play.NodeService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public abstract class NodeManager extends BaseManager {

    protected Promise<Boolean> exists(String label, JsonNode properties) {
        Promise<JsonNode> json = get(label, properties);
        return json.map(
            new Function<JsonNode, Boolean>() {
                public Boolean apply(JsonNode json) {
                    return json.get("data").size() > 0;
                }
            });
    }

    protected Promise<List<JsonNode>> all(String label) {
        Promise<WS.Response> response = NodeService.getNodes(label);
        return response.map(
            new Function<WS.Response, List<JsonNode>>() {
                public List<JsonNode> apply(WS.Response response) {
                    JsonNode json = response.asJson();
                    return json.get("data").findValues("data");
                }
            });
    }

    protected Promise<JsonNode> get(String label, JsonNode properties) {
        Promise<WS.Response> response = NodeService
            .getNode(label, properties);
        return response.map(new JsonFunction()); // JsonFunction should return(something like) json.get("data"), cf. `Function` for `all` above
    }

    public Promise<Boolean> create(final JsonNode properties) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> created = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> created = create(properties, location);
                    return created.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean created) {
                                if (created) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return created;
    };

    protected Promise<Boolean> update(
        String label, JsonNode oldProperties, JsonNode newProperties) {
        Promise<WS.Response> response = NodeService.updateNode(
            label, oldProperties, newProperties);
        return response.map(new SuccessFunction());
    }

    public Promise<Boolean> delete(final JsonNode properties) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> deleted = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> deleted = delete(properties, location);
                    return deleted.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean deleted) {
                                if (deleted) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return deleted;
    };

    public Promise<Boolean> connect(
        final JsonNode startNode, final JsonNode endNode) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> connected = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> connected =
                        connect(startNode, endNode, location);
                    return connected.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean connected) {
                                if (connected) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return connected;
    }

    public abstract Promise<Boolean> exists(JsonNode properties);

    public abstract Promise<? extends List<? extends Node>> all();

    public abstract Promise<? extends Node> get(JsonNode properties);

    protected abstract Promise<Boolean> create(
        JsonNode properties, String location);

    public abstract Promise<Boolean> update(
        JsonNode oldProperties, JsonNode newProperties);

    protected abstract Promise<Boolean> delete(
        JsonNode properties, String location);

    protected abstract Promise<Boolean> connect(
        JsonNode startNode, JsonNode endNode, String location);

}
