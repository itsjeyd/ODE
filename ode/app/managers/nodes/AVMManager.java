package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
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

    @Override
    protected Promise<Boolean> delete(
        final JsonNode properties, final String location) {
        // 1. Remove all features
        Promise<Boolean> emptied = Substructure.nodes
            .empty(properties, location);
        // 2. Delete AVM
        Promise<Boolean> deleted = emptied.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean emptied) {
                    if (emptied) {
                        ObjectNode props = (ObjectNode) properties.deepCopy();
                        return AVMManager.super
                            .delete(props.retain("uuid"), location);
                    }
                    return Promise.pure(false);
                }
            });
        return deleted;
    }

    protected Promise<Boolean> empty(
        final JsonNode properties, final String location) {
        // 1. Get list of all features
        Substructure avm = new Substructure(properties.get("uuid").asText());
        Promise<List<JsonNode>> features = Has.relationships
            .endNodes(avm, location);
        // 2. Remove each one of them from the AVM
        Promise<Boolean> emptied = features.flatMap(
            new Function<List<JsonNode>, Promise<Boolean>>() {
                public Promise<Boolean> apply(List<JsonNode> features) {
                    return removeFeatures(properties, features, location);
                }
            });
        return emptied;
    }

    private Promise<Boolean> removeFeatures(
        final JsonNode properties, final List<JsonNode> features,
        final String location) {
        Promise<Boolean> removed = Promise.pure(true);
        for (final JsonNode feature: features) {
            removed = removed.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean removed) {
                        if (removed) {
                            return removeFeature(properties, feature, location);
                        }
                        return Promise.pure(false);
                    }
                });
        }
        return removed;
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

    public Promise<Boolean> removeFeature(
        final JsonNode avm, final JsonNode feature) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> removed = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> removed =
                        removeFeature(avm, feature, location);
                    return removed.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean removed) {
                                if (removed) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return removed;
    }

    protected Promise<Boolean> removeFeature(
        final JsonNode avm, final JsonNode feature, final String location) {
        final String uuid = avm.get("uuid").asText();
        final Substructure a = new Substructure(uuid);
        final String name = feature.get("name").asText();
        final Feature f = new Feature(name);
        // 1. Disconnect AVM from feature
        Promise<Boolean> removed = Has.relationships
            .delete(a, f, location);
        // 2. Disconnect feature from value
        removed = removed.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean removed) {
                    if (removed) {
                        final ObjectNode props = Json.newObject();
                        props.put("rule", avm.get("ruleUUID").asText());
                        if (feature.get("type").asText().equals("atomic")) {
                            props.put("avm", uuid);
                        }
                        Promise<Boolean> r = Has.relationships
                            .delete(f, props, location);
                        // 3. If feature is complex, delete value
                        if (feature.get("type").asText().equals("complex")) {
                            final String subUUID =
                                UUIDGenerator.from(uuid + name);
                            r = r.flatMap(
                                new Function<Boolean, Promise<Boolean>>() {
                                    public Promise<Boolean> apply(Boolean r) {
                                        if (r) {
                                            ObjectNode properties =
                                                Json.newObject();
                                            properties.put("uuid", subUUID);
                                            properties.put(
                                                "ruleUUID",
                                                avm.get("ruleUUID").asText());
                                            return Substructure.nodes
                                                .delete(properties, location);
                                        }
                                        return Promise.pure(false);
                                    }
                                });
                        }
                        return r;
                    }
                    return Promise.pure(false);
                }
            });
        return removed;
    }

}
