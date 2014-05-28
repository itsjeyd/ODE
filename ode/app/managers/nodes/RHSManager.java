package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import models.nodes.CombinationGroup;
import models.nodes.RHS;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;


public class RHSManager extends UUIDNodeManager {

    public RHSManager() {
        this.label = "RHS";
    }

    @Override
    protected Promise<Boolean> create(
        JsonNode properties, final String location) {
        final RHS rhs = new RHS(properties.get("uuid").asText());
        // 1. Create RHS
        Promise<Boolean> created = super.create(properties, location);
        // 2. Generate UUID for combination group
        final String uuid = UUID.randomUUID().toString();
        // 3. Create default combination group
        created = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        int position = 1;
                        ObjectNode props = Json.newObject();
                        props.put("uuid", uuid);
                        props.put("position", position);
                        return CombinationGroup.nodes
                            .create(props, location);
                    }
                    return Promise.pure(false);
                }
            });
        // 3. Connect RHS to combination group
        created = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        CombinationGroup group = new CombinationGroup(uuid);
                        return Has.relationships.create(rhs, group, location);
                    }
                    return Promise.pure(false);
                }
            });
        return created;
    }

    protected Promise<Boolean> connect(
        JsonNode rhs, JsonNode group, String location) {
        RHS r = new RHS(rhs.get("uuid").asText());
        CombinationGroup g = new CombinationGroup(
            group.get("uuid").asText(), group.get("position").asInt());
        return Has.relationships.create(r, g, location);
    }

    protected Promise<Boolean> disconnect(
        JsonNode rhs, final JsonNode group, final String location) {
        RHS r = new RHS(rhs.get("uuid").asText());
        CombinationGroup g = new CombinationGroup(group.get("uuid").asText());
        // 1. Disconnect RHS from group
        Promise<Boolean> disconnected = Has.relationships
            .delete(r, g, location);
        // 2. Delete group
        disconnected = disconnected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean disconnected) {
                    if (disconnected) {
                        return CombinationGroup.nodes.delete(group, location);
                    }
                    return Promise.pure(false);
                }
            });
        return disconnected;
    }

}
