package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Function;
import play.libs.F.Promise;


public abstract class ContentNodeManager
    extends LabeledNodeWithPropertiesManager {

    // READ

    @Override
    public Promise<Boolean> exists(JsonNode properties) {
        return super.exists(properties, "uuid");
    }

    // CREATE

    @Override
    public Promise<Boolean> create(
        final JsonNode properties, final String location) {
        Promise<Boolean> exists = exists(properties);
        Promise<Boolean> created = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return Promise.pure(true);
                    }
                    return ContentNodeManager.super
                        .create(properties, location);
                }
            });
        return created;
    }

}
