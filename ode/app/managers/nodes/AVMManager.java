package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.nodes.Feature;
import models.nodes.Substructure;
import models.nodes.Value;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import utils.UUIDGenerator;


public class AVMManager extends UUIDNodeManager {

    public AVMManager() {
        this.label = "AVM";
    }

    protected Promise<Boolean> connect(
        final JsonNode avm, final JsonNode feature, final String location) {
        final String uuid = avm.get("uuid").asText();
        Substructure a = new Substructure(uuid);
        final String name = feature.get("name").asText();
        final Feature f = new Feature(name);
        final ObjectNode props = Json.newObject();
        props.put("rule", avm.get("ruleUUID"));
        // 1. Connect AVM to feature
        Promise<Boolean> connected = Has.relationships
            .create(a, f, props, location);
        // 2. If feature is
        //    - atomic, connect to default value ("underspecified")
        //    - complex,
        //      a. create substructure
        //      b. connect feature to substructure
        String type = feature.get("type").asText();
        if (type.equals("atomic")) {
            connected = connected.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean connected) {
                        if (connected) {
                            Value v = new Value("underspecified");
                            props.put("avm", uuid);
                            return Has.relationships
                                .create(f, v, props, location);
                        }
                        return Promise.pure(false);
                    }
                });
        } else if (type.equals("complex")) {
            final String subUUID = UUIDGenerator.from(uuid + name);
            connected = connected.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean connected) {
                        if (connected) {
                            ObjectNode properties = Json.newObject();
                            properties.put("uuid", subUUID);
                            return Substructure.nodes
                                .create(properties, location);
                        }
                        return Promise.pure(false);
                    }
                });
            connected = connected.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean connected) {
                        if (connected) {
                            Substructure s = new Substructure(subUUID);
                            return Has.relationships
                                .create(f, s, props, location);
                        }
                        return Promise.pure(false);
                    }
                });
        }
        return connected;
    }

    public Promise<Boolean> setValue(
        JsonNode avm, JsonNode feature, JsonNode value,
        final JsonNode newValue) {
        final ObjectNode props = Json.newObject();
        props.put("rule", avm.get("ruleUUID").asText());
        props.put("avm", avm.get("uuid").asText());
        final Feature f = new Feature(feature.get("name").asText());
        final Value v = new Value(value.get("name").asText());
        // 1. Establish transaction
        Promise<String> location = beginTransaction();
        Promise<Boolean> updated = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    // 2. Delete :HAS relationship between feature and current value
                    Promise<Boolean> deleted = Has.relationships
                        .delete(f, v, props, location);
                    // 3. Create :HAS relationship between feature and new value
                    Promise<Boolean> created = deleted.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean deleted) {
                                if (deleted) {
                                    Value n = new Value(
                                        newValue.get("name").asText());
                                    return Has.relationships
                                        .create(f, n, props, location);
                                }
                                return Promise.pure(false);
                            }
                        });
                    // 4. Finalize transaction
                    Promise<Boolean> updated = created.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean created) {
                                if (created) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                    return updated;
                }
            });
        return updated;
    }

}
