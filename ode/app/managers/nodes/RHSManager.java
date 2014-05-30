package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import models.nodes.CombinationGroup;
import models.nodes.RHS;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;


public class RHSManager extends CollectionNodeManager {

    public RHSManager() {
        this.label = "RHS";
    }

    // READ

    public Promise<RHS> get(JsonNode properties) {
        final RHS rhs = new RHS(properties.get("uuid").asText());
        Promise<JsonNode> json = toJSON(properties);
        return json.map(
            new Function<JsonNode, RHS>() {
                public RHS apply(JsonNode json) {
                    rhs.json = json;
                    return rhs;
                }
            });
    }

    private Promise<JsonNode> toJSON(final JsonNode properties) {
        Promise<List<JsonNode>> groups = groups(properties);
        Promise<JsonNode> json = groups.map(
            new Function<List<JsonNode>, JsonNode>() {
                public JsonNode apply(List<JsonNode> groups) {
                    ArrayNode groupNodes =
                        JsonNodeFactory.instance.arrayNode();
                    groupNodes.addAll(groups);
                    ((ObjectNode) properties).put("groups", groupNodes);
                    return properties;
                }
            });
        return json;
    }

    public Promise<List<JsonNode>> groups(JsonNode properties) {
        RHS rhs = new RHS(properties.get("uuid").asText());
        Promise<List<JsonNode>> nodes = Has.relationships.endNodes(rhs);
        Promise<List<JsonNode>> groups = nodes.flatMap(
            new Function<List<JsonNode>, Promise<List<JsonNode>>>() {
                public Promise<List<JsonNode>> apply(List<JsonNode> nodes) {
                    List<Promise<? extends JsonNode>> groups =
                        new ArrayList<Promise<? extends JsonNode>>();
                    for (JsonNode node: nodes) {
                        Promise<JsonNode> group = CombinationGroup.nodes
                            .toJSON(node);
                        groups.add(group);
                    }
                    return Promise.sequence(groups);
                }
            });
        return groups;
    }

    // CREATE

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

    // DELETE

    protected Promise<Boolean> empty(JsonNode properties, String location) {
        RHS rhs = new RHS(properties.get("uuid").asText());
        return super.empty(rhs, location);
    }

    // Connections to other nodes

    protected Promise<Boolean> connect(
        final JsonNode rhs, final JsonNode group, final String location) {
        Promise<Boolean> created = CombinationGroup.nodes
            .create(group, location);
        Promise<Boolean> connected = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        RHS r = new RHS(rhs.get("uuid").asText());
                        CombinationGroup g = new CombinationGroup(
                            group.get("uuid").asText(),
                            group.get("position").asInt());
                        return Has.relationships.create(r, g, location);
                    }
                    return Promise.pure(false);
                }
            });
        return connected;
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
