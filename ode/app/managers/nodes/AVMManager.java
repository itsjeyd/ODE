package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import constants.NodeType;
import java.util.ArrayList;
import java.util.List;
import models.nodes.Feature;
import models.nodes.Substructure;
import models.nodes.Value;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.Json;
import utils.UUIDGenerator;


public class AVMManager extends LabeledNodeWithPropertiesManager {

    public AVMManager() {
        this.label = NodeType.AVM.toString();
    }

    // UPDATE

    public Promise<Boolean> setValue(
        final JsonNode avm, final JsonNode feature, final JsonNode value,
        final JsonNode newValue) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> updated = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> updated =
                        setValue(avm, feature, value, newValue, location);
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

    private Promise<Boolean> setValue(
        JsonNode avm, JsonNode feature, JsonNode value,
        final JsonNode newValue, final String location) {
        final ObjectNode props = Json.newObject();
        props.put("rule", avm.get("ruleUUID").asText());
        props.put("avm", avm.get("uuid").asText());
        final Feature f = new Feature(feature.get("name").asText());
        final Value v = new Value(value.get("name").asText());
        // 1. Delete :HAS relationship between feature and current value
        Promise<Boolean> updated = Has.relationships
            .delete(f, v, props, location);
        // 2. Create :HAS relationship between feature and new value
        updated = updated.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean updated) {
                    if (updated) {
                        Value n = new Value(
                            newValue.get("name").asText());
                        return Has.relationships
                            .create(f, n, props, location);
                    }
                    return Promise.pure(false);
                }
            });
        return updated;
    }

    // DELETE

    @Override
    protected Promise<Boolean> delete(
        final JsonNode properties, final String location) {
        // 1. Remove all features
        Promise<Boolean> emptied = empty(properties, location);
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

    private Promise<Boolean> empty(
        final JsonNode properties, final String location) {
        // 1. Get list of all features
        Substructure avm = new Substructure(properties.get("uuid").asText());
        Promise<List<JsonNode>> features = Has.relationships
            .endNodes(avm, location);
        // 2. Remove each one of them from the AVM
        Promise<Boolean> emptied = features.flatMap(
            new Function<List<JsonNode>, Promise<Boolean>>() {
                public Promise<Boolean> apply(List<JsonNode> features) {
                    return disconnect(properties, features, location);
                }
            });
        return emptied;
    }

    private Promise<Boolean> disconnect(
        final JsonNode properties, final List<JsonNode> features,
        final String location) {
        Promise<Boolean> removed = Promise.pure(true);
        for (final JsonNode feature: features) {
            removed = removed.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean removed) {
                        if (removed) {
                            return disconnect(properties, feature, location);
                        }
                        return Promise.pure(false);
                    }
                });
        }
        return removed;
    }

    // Connections to other nodes

    @Override
    protected Promise<Boolean> connect(
        JsonNode parentAVM, JsonNode feature, String location) {
        String parentUUID = parentAVM.get("uuid").asText();
        // 1. Connect AVM to feature
        Substructure avm = new Substructure(parentUUID);
        Feature feat = new Feature(feature.get("name").asText());
        ObjectNode props = Json.newObject();
        props.put("rule", parentAVM.get("ruleUUID"));
        Promise<Boolean> connected = Has.relationships
            .create(avm, feat, props, location);
        // 2. If feature is
        //    - atomic, connect to default value ("underspecified")
        //    - complex,
        //      a. create substructure
        //      b. connect feature to substructure
        String type = feature.get("type").asText();
        if (type.equals("atomic")) {
            connected =
                connectToValue(feat, props, parentUUID, connected, location);
        } else if (type.equals("complex")) {
            connected =
                connectToAVM(feature, props, parentUUID, connected, location);
        }
        return connected;
    }

    private Promise<Boolean> connectToValue(
        final Feature feat, final ObjectNode props, final String parentUUID,
        Promise<Boolean> connected, final String location) {
        return connected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean connected) {
                    if (connected) {
                        Value value = new Value("underspecified");
                        props.put("avm", parentUUID);
                        return Has.relationships
                            .create(feat, value, props, location);
                    }
                    return Promise.pure(false);
                }
            });
    }

    private Promise<Boolean> connectToAVM(
        final JsonNode feature, final JsonNode props, final String parentUUID,
        Promise<Boolean> connected, final String location) {
        Promise<Feature> feat = Feature.nodes.get(feature);
        return connected.zip(feat).flatMap(
            new Function<Tuple<Boolean, Feature>, Promise<Boolean>>() {
                public Promise<Boolean> apply(Tuple<Boolean, Feature> t) {
                    Boolean connected = t._1;
                    if (connected) {
                        final Feature feat = t._2;
                        final String subUUID = UUIDGenerator
                            .from(parentUUID + feat.getUUID());
                        ObjectNode properties = Json.newObject();
                        properties.put("uuid", subUUID);
                        Promise<Boolean> created =
                            create(properties, location);
                        return created.flatMap(
                            new Function<Boolean, Promise<Boolean>>() {
                                public Promise<Boolean> apply(Boolean created) {
                                    if (created) {
                                        Substructure avm =
                                            new Substructure(subUUID);
                                        return Has.relationships.create(
                                            feat, avm, props, location);
                                    }
                                    return Promise.pure(false);
                                }
                            });
                    }
                    return Promise.pure(false);
                }
            });
    }

    @Override
    protected Promise<Boolean> disconnect(
        final JsonNode avm, final JsonNode feature, final String location) {
        final String uuid = avm.get("uuid").asText();
        final Substructure a = new Substructure(uuid);
        String name = feature.get("name").asText();
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
                        return Has.relationships.delete(f, props, location);
                    }
                    return Promise.pure(false);
                }
            });
        // 3. If feature is complex, delete value
        if (feature.get("type").asText().equals("complex")) {
            final String subUUID = UUIDGenerator.from(uuid + name);
            removed = removed.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean removed) {
                        if (removed) {
                            ObjectNode properties = Json.newObject();
                            properties.put("uuid", subUUID);
                            properties.put(
                                "ruleUUID", avm.get("ruleUUID").asText());
                            return delete(properties, location);
                        }
                        return Promise.pure(false);
                    }
                });
        }
        return removed;
    }

    // Custom functionality

    protected Promise<JsonNode> toJSON(final JsonNode properties) {
        // 1. Get list of all pairs
        Promise<List<Pair>> pairs = pairs(properties);
        // 2. Convert pairs to JSON
        Promise<JsonNode> json = pairs.map(
            new Function<List<Pair>, JsonNode>() {
                public JsonNode apply(List<Pair> pairs) {
                    ArrayNode pairNodes =
                        JsonNodeFactory.instance.arrayNode();
                    for (Pair pair: pairs) {
                        pairNodes.add(pair.toJSON());
                    }
                    ((ObjectNode) properties).put("pairs", pairNodes);
                    return properties;
                }
            });
        return json;
    }

    private Promise<List<Pair>> pairs(final JsonNode properties) {
        Promise<List<JsonNode>> features = features(properties);
        Promise<List<Pair>> pairs = features.flatMap(
            new Function<List<JsonNode>, Promise<List<Pair>>>() {
                public Promise<List<Pair>> apply(List<JsonNode> features) {
                    List<Promise<? extends Pair>> pairs =
                        new ArrayList<Promise<? extends Pair>>();
                    for (JsonNode feature: features) {
                        Promise<JsonNode> attribute = Promise.pure(feature);
                        Promise<JsonNode> value =
                            getValue(properties, feature);
                        Promise<Pair> pair = Pair.of(attribute, value);
                        pairs.add(pair);
                    }
                    return Promise.sequence(pairs);
                }
            });
        return pairs;
    }

    private Promise<List<JsonNode>> features(JsonNode properties) {
        Substructure avm = new Substructure(properties.get("uuid").asText());
        // 1. Get list of all features
        Promise<List<JsonNode>> nodes = Has.relationships.endNodes(avm);
        // 2. For each feature, collect targets
        Promise<List<JsonNode>> features = nodes.flatMap(
            new Function<List<JsonNode>, Promise<List<JsonNode>>>() {
                public Promise<List<JsonNode>> apply(List<JsonNode> nodes) {
                    List<Promise<? extends JsonNode>> features =
                        new ArrayList<Promise<? extends JsonNode>>();
                    for (final JsonNode node: nodes) {
                        ((ObjectNode) node).retain("name", "type");
                        Promise<List<String>> targets = Feature.nodes
                            .targets(node);
                        Promise<JsonNode> feature = targets.map(
                            new Function<List<String>, JsonNode>() {
                                public JsonNode apply(List<String> targets) {
                                    ArrayNode targetNodes =
                                    JsonNodeFactory.instance.arrayNode();
                                    for (String target: targets) {
                                        targetNodes.add(target);
                                    }
                                    ((ObjectNode) node)
                                        .put("targets", targetNodes);
                                    return node;
                                }
                            });
                        features.add(feature);
                    }
                    return Promise.sequence(features);
                }
            });
        return features;
    }

    private Promise<JsonNode> getValue(JsonNode avm, JsonNode feature) {
        String uuid = avm.get("uuid").asText();
        String ruleUUID = avm.get("ruleUUID").asText();
        if (feature.get("type").asText().equals("complex")) {
            String name = feature.get("name").asText();
            String subUUID = UUIDGenerator.from(uuid + name);
            ObjectNode substructure = Json.newObject();
            substructure.put("uuid", subUUID);
            substructure.put("ruleUUID", ruleUUID);
            return toJSON(substructure);
        }
        Feature f = new Feature(feature.get("name").asText());
        ObjectNode properties = Json.newObject();
        properties.put("rule", ruleUUID);
        properties.put("avm", uuid);
        Promise<JsonNode> node = Has.relationships.endNode(f, properties);
        node = node.map(
            new Function<JsonNode, JsonNode>() {
                public JsonNode apply(JsonNode node) {
                    String name = node.get("name").asText();
                    return new TextNode(name);
                }
            });
        return node;
    }


    private static class Pair {
        public JsonNode attribute;
        public JsonNode value;
        private Pair(JsonNode attribute, JsonNode value) {
            this.attribute = attribute;
            this.value = value;
        }
        public static Promise<Pair> of(
            Promise<JsonNode> attribute, Promise<JsonNode> value) {
            return attribute.zip(value).map(
                new Function<Tuple<JsonNode, JsonNode>, Pair>() {
                    public Pair apply(Tuple<JsonNode, JsonNode> pair) {
                        return new Pair(pair._1, pair._2);
                    }
                });
        }
        public JsonNode toJSON() {
            ObjectNode json = Json.newObject();
            json.put("attribute", this.attribute);
            json.put("value", this.value);
            return json;
        }
    }

}
