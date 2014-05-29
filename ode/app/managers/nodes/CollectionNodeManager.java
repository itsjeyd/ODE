package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import models.nodes.LabeledNodeWithProperties;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;


public abstract class CollectionNodeManager
    extends LabeledNodeWithPropertiesManager {

    // DELETE

    @Override
    protected Promise<Boolean> delete(
        final JsonNode properties, final String location) {
        // 1. Empty collection
        Promise<Boolean> emptied = empty(properties, location);
        // 2. Delete collection
        Promise<Boolean> deleted = emptied.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean emptied) {
                    if (emptied) {
                        return CollectionNodeManager.super
                            .delete(properties, location);
                    }
                    return Promise.pure(false);
                }
            });
        return deleted;
    }

    protected Promise<Boolean> empty(
        final LabeledNodeWithProperties collectionNode,
        final String location) {
        // 1. Get list of all nodes belonging to this collection
        Promise<List<JsonNode>> items = Has.relationships
            .endNodes(collectionNode, location);
        // 2. Remove each one of them from the collection
        Promise<Boolean> emptied = items.flatMap(
            new Function<List<JsonNode>, Promise<Boolean>>() {
                public Promise<Boolean> apply(List<JsonNode> items) {
                    return disconnect(
                        collectionNode.getProperties(), items, location);
                }
            });
        return emptied;
    }

    protected Promise<Boolean> disconnect(
        final JsonNode properties, List<JsonNode> items, final
        String location) {
        Promise<Boolean> disconnected = Promise.pure(true);
        for (final JsonNode item: items) {
            disconnected = disconnected.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean disconnected) {
                        if (disconnected) {
                            return disconnect(properties, item, location);
                        }
                        return Promise.pure(false);
                    }
                });
        }
        return disconnected;
    }

    protected abstract Promise<Boolean> empty(
        JsonNode properties, String location);

}
