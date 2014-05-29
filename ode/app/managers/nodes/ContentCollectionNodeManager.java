package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Function;
import play.libs.F.Promise;


public abstract class ContentCollectionNodeManager
    extends CollectionNodeManager {

    // UPDATE

    public Promise<Boolean> update(
        final JsonNode collectionNode, final JsonNode oldContent,
        final JsonNode newContent) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> updated = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> updated = update(
                        collectionNode, oldContent, newContent, location);
                    return updated.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean updated) {
                                if (updated) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return updated;
    }

    protected Promise<Boolean> update(
        final JsonNode collectionNode, JsonNode oldContent,
        final JsonNode newContent, final String location) {
        // 1. Disconnect node from old content
        Promise<Boolean> updated =
            disconnect(collectionNode, oldContent, location);
        // 2. Connect node to new content
        updated = updated.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean updated) {
                    if (updated) {
                        return connect(collectionNode, newContent, location);
                    }
                    return Promise.pure(false);
                }
            });
        return updated;
    }

}
