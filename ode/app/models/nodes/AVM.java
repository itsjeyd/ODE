package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import constants.NodeType;
import managers.nodes.AVMManager;
import models.relationships.HasFeatureRelationship;


public abstract class AVM extends UUIDNode {
    public Rule rule;
    public JsonNode json;

    protected AVM() {
        super(NodeType.AVM);
    }

    public AVM(Rule rule) {
        this();
        this.rule = rule;
    }

    public abstract Promise<Boolean> create();

    public Promise<List<Feature>> getFeatures() {
        return HasFeatureRelationship.getEndNodes(this);
    }

    public Promise<List<Pair>> getPairs() {
        final AVM avm = this;
        Promise<List<Feature>> features = this.getFeatures();
        return features.flatMap(
            new Function<List<Feature>, Promise<List<Pair>>>() {
                public Promise<List<Pair>> apply(List<Feature> features) {
                    List<Promise<? extends Pair>> pairs =
                        new ArrayList<Promise<? extends Pair>>();
                    for (Feature feature: features) {
                        Promise<JsonNode> attribute = feature.toJSON();
                        Promise<JsonNode> value = feature
                            .getValue(avm.rule, avm);
                        Promise<Pair> pair = Pair.of(attribute, value);
                        pairs.add(pair);
                    }
                    return Promise.sequence(pairs);
                }
            });
    }

    public Promise<JsonNode> toJSON() {
        final AVM avm = this;
        final ObjectNode json = Json.newObject();
        Promise<UUID> uuid = this.getUUID();
        return uuid.flatMap(
            new Function<UUID, Promise<JsonNode>>() {
                public Promise<JsonNode> apply(UUID uuid) {
                    json.put("uuid", uuid.toString());
                    Promise<List<Pair>> pairs = avm.getPairs();
                    return pairs.map(
                        new Function<List<Pair>, JsonNode>() {
                            public JsonNode apply(List<Pair> pairs) {
                                ArrayNode pairNodes =
                                    JsonNodeFactory.instance.arrayNode();
                                for (Pair pair: pairs) {
                                    pairNodes.add(pair.toJSON());
                                }
                                json.put("pairs", pairNodes);
                                return json;
                            }
                        });
                }
            });
    }

    public Promise<Boolean> add(final Feature feature) {
        final AVM avm = this;
        Promise<Boolean> connected =
            new HasFeatureRelationship(avm, feature).create();
        return connected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean connected) {
                    if (connected) {
                        return feature.addDefaultValue(avm.rule, avm);
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> empty() {
        final AVM avm = this;
        Promise<List<Feature>> features = this.getFeatures();
        Promise<List<Boolean>> removed = features.flatMap(
            new Function<List<Feature>, Promise<List<Boolean>>>() {
                public Promise<List<Boolean>> apply(List<Feature> features) {
                    List<Promise<? extends Boolean>> removed =
                        new ArrayList<Promise<? extends Boolean>>();
                    for (Feature feature: features) {
                        removed.add(feature.remove(avm.rule, avm));
                    }
                    return Promise.sequence(removed);
                }
            });
        return removed.map(
            new Function<List<Boolean>, Boolean>() {
                public Boolean apply(List<Boolean> removed) {
                    for (Boolean r: removed) {
                        if (!r) {
                            return false;
                        }
                    }
                    return true;
                }
            });
    }

    public Promise<Boolean> delete() {
        return AVMManager.delete(this);
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
