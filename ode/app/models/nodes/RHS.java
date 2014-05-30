package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;
import java.nio.charset.Charset;
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
