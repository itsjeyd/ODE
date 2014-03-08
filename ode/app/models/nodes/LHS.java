package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import managers.nodes.LHSManager;
import models.relationships.HasRelationship;
import models.relationships.LHSRelationship;


public class LHS extends AVM {
    public Rule parent;

    public LHS(Rule rule) {
        super(rule);
        this.parent = rule;
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.map(new UUIDFunction());
    }

    public Promise<Boolean> create() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.flatMap(new CreateFunction(this));
    }

    public Promise<Boolean> connectTo(Rule embeddingRule) {
        return new LHSRelationship(embeddingRule, this).create();
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
                        return feature.addDefaultValue(lhs.rule, lhs);
                    }
                    return Promise.pure(false);
                }
            });
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
