package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.LHSManager;
import models.relationships.HasRelationship;
import models.relationships.LHSRelationship;


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
        Promise<Boolean> connected = uuid.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID uuid) {
                    lhs.jsonProperties.put("uuid", uuid.toString());
                    return new HasRelationship(lhs, feature).create();
                }
            });
        return connected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean connected) {
                    if (connected) {
                        lhs.rule.lhs = lhs;
                        return feature.addDefaultValue(lhs.rule);
                    }
                    return Promise.pure(false);
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
            final JsonNode data = json.get("data");
            if (data.size() > 0) {
                Promise<UUID> uuid = this.lhs.getUUID();
                Promise<JsonNode> structure = uuid.map(
                    new Function<UUID, JsonNode>() {
                        public JsonNode apply(UUID uuid) {
                            ObjectNode structure = Json.newObject();
                            for (JsonNode row: data) {
                                for (JsonNode column: row) {
                                    JsonNode startNode = column.get(0)
                                        .get("data");
                                    JsonNode endNode = column.get(1)
                                        .get("data");
                                    if (startNode.has("uuid")) {
                                        String featureName = endNode
                                            .get("name").asText();
                                        structure.put(
                                            featureName, Json.newObject());
                                    }
                                }
                            }
                            return structure;
                        }
                    });
                final LHS ruleLHS = this.lhs;
                return structure.map(
                    new Function<JsonNode, LHS>() {
                        public LHS apply(JsonNode structure) {
                            ruleLHS.json = structure;
                            return ruleLHS;
                        }
                    });
            } else {
                ObjectNode pairs = Json.newObject();
                this.lhs.json = pairs;
                return Promise.pure(this.lhs);
            }
        }
    }

}
