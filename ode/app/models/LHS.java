package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.LHSManager;


public class LHS extends LabeledNodeWithProperties {
    public Rule rule;
    public JsonNode json;

    private LHS() {
        this.label = NodeType.AVM;
        this.jsonProperties = Json.newObject();
    }

    public LHS(Rule rule) {
        this();
        this.rule = rule;
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> ruleUUID = this.rule.getUUID();
        return ruleUUID.map(
            new Function<UUID, UUID>() {
                public UUID apply(UUID ruleUUID) {
                    byte[] bytes = ruleUUID.toString()
                        .getBytes(Charset.forName("UTF-8"));
                    return UUID.nameUUIDFromBytes(bytes);
                }
            });
    }

    public Promise<Boolean> create() {
        Promise<UUID> ruleUUID = this.rule.getUUID();
        Promise<Boolean> created = ruleUUID
            .flatMap(new CreateFunction(this));
        return created.flatMap(new ConnectToRuleFunction(this));
    }

    public Promise<LHS> get() {
        Promise<JsonNode> json = LHSManager.get(this);
        return json.flatMap(new GetFunction(this));
    }

    public Promise<Boolean> add(final Feature feature) {
        final LHS lhs = this;
        Promise<UUID> uuid = this.getUUID();
        return uuid.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID uuid) {
                    lhs.jsonProperties.put("uuid", uuid.toString());
                    return new HasRelationship(lhs, feature).create();
                }
            });
    }

    private class CreateFunction implements
                                     Function<UUID, Promise<Boolean>> {
        private LHS lhs;
        public CreateFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<Boolean> apply(UUID ruleUUID) {
            byte[] bytes = ruleUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            UUID uuid = UUID.nameUUIDFromBytes(bytes);
            this.lhs.jsonProperties.put("uuid", uuid.toString());
            return LHSManager.create(this.lhs);
        }
    }

    private class ConnectToRuleFunction
        implements Function<Boolean, Promise<Boolean>> {
        private LHS lhs;
        public ConnectToRuleFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<Boolean> apply(Boolean created) {
            return new LHSRelationship(this.lhs).create();
        }
    }

    private class GetFunction implements Function<JsonNode, Promise<LHS>> {
        private LHS lhs;
        public GetFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<LHS> apply(JsonNode json) {
            JsonNode data = json.get("data");
            if (data.size() > 0) {
                // Process node-relationship-node triples appropriately;
                // for now just get names of end nodes (i.e.,
                // features) and add them to pairs with arbitrary
                // default value
                List<JsonNode> endNodes = data.findValues("end");
                List<Promise<? extends Feature>> featureList =
                    new ArrayList<Promise<? extends Feature>>();
                for (JsonNode endNode: endNodes) {
                    Promise<Feature> feature = Feature
                        .getByURL(endNode.asText());
                    featureList.add(feature);
                }
                Promise<List<Feature>> features = Promise
                    .sequence(featureList);
                Promise<ObjectNode> pairs = features.map(
                    new Function<List<Feature>, ObjectNode>() {
                        public ObjectNode apply(List<Feature> features) {
                            ObjectNode pairs = Json.newObject();
                            for (Feature feature: features) {
                                ObjectNode value = Json.newObject();
                                pairs.put(feature.name, value);
                            }
                            return pairs;
                        }
                    });
                return pairs.map(new SetPairsFunction(this.lhs));
            } else {
                ObjectNode pairs = Json.newObject();
                this.lhs.json = pairs;
                return Promise.pure(this.lhs);
            }
        }
    }

    private static class SetPairsFunction implements
                                            Function<ObjectNode, LHS> {
        private LHS lhs;
        public SetPairsFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public LHS apply(ObjectNode pairs) {
            this.lhs.json = pairs;
            return this.lhs;
        }
    }

}
