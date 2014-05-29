package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.NodeType;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import managers.nodes.RHSManager;
import models.relationships.GroupRelationship;
import play.libs.F.Function;
import play.libs.F.Promise;


public class RHS extends UUIDNode {

    public static final RHSManager nodes = new RHSManager();

    public Rule rule;
    public JsonNode json;

    private RHS() {
        super(NodeType.RHS);
    }

    public RHS(Rule rule) {
        this();
        this.rule = rule;
    }

    public RHS(String uuid) {
        this();
        this.jsonProperties.put("uuid", uuid);
    }

    public RHS(Rule rule, String uuid) {
        this(rule);
        this.jsonProperties.put("uuid", uuid);
    }

    public Promise<JsonNode> toJSON() {
        Promise<List<JsonNode>> groups = this.getGroups().flatMap(
            new Function<List<CombinationGroup>, Promise<List<JsonNode>>>() {
                public Promise<List<JsonNode>> apply(
                    List<CombinationGroup> groups) {
                    List<Promise<? extends JsonNode>> groupList =
                        new ArrayList<Promise<? extends JsonNode>>();
                    for (CombinationGroup group: groups) {
                        Promise<JsonNode> groupJSON = group.toJSON();
                        groupList.add(groupJSON);
                    }
                    return Promise.sequence(groupList);
                }
            });
        final ObjectNode json = this.jsonProperties.deepCopy();
        return groups.map(
            new Function<List<JsonNode>, JsonNode>() {
                public JsonNode apply(List<JsonNode> groupList) {
                    ArrayNode groups = JsonNodeFactory.instance.arrayNode();
                    groups.addAll(groupList);
                    json.put("groups", groups);
                    return json;
                }
            });
    }

    public Promise<RHS> get() {
        Promise<UUID> uuid = this.getUUID();
        Promise<JsonNode> json = uuid.flatMap(
            new Function<UUID, Promise<JsonNode>>() {
                public Promise<JsonNode> apply(UUID uuid) {
                    RHS.this.jsonProperties.put("uuid", uuid.toString());
                    return RHS.this.toJSON();
                }
            });
        return json.map(
            new Function<JsonNode, RHS>() {
                public RHS apply(JsonNode json) {
                    RHS.this.json = json;
                    return RHS.this;
                }
            });
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> parentUUID = this.rule.getUUID();
        return parentUUID.map(new UUIDFunction());
    }

    protected Promise<List<CombinationGroup>> getGroups() {
        if (!this.jsonProperties.has("uuid")) {
            Promise<UUID> uuid = this.getUUID();
            return uuid.flatMap(
                new Function<UUID, Promise<List<CombinationGroup>>>() {
                    public Promise<List<CombinationGroup>> apply(UUID uuid) {
                        RHS.this.jsonProperties.put("uuid", uuid.toString());
                        return RHS.this.getGroups();
                    }
                });
        }
        return GroupRelationship.getEndNodes(this);
    }


    private static class UUIDFunction implements Function<UUID, UUID> {
        public UUID apply(UUID ruleUUID) {
            byte[] bytes = ruleUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            return UUID.nameUUIDFromBytes(bytes);
        }
    }

}
