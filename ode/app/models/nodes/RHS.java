package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.RHSManager;
import models.relationships.RHSRelationship;


public class RHS extends LabeledNodeWithProperties {
    public Rule rule;

    private RHS() {
        this.label = NodeType.RHS;
        this.jsonProperties = Json.newObject();
    }

    public RHS(Rule rule) {
        this();
        this.rule = rule;
    }

    public Promise<Boolean> create() {
        Promise<UUID> ruleUUID = this.rule.getUUID();
        return ruleUUID.flatMap(new CreateFunction(this));
    }

    public Promise<Boolean> connectTo(Rule embeddingRule) {
        return new RHSRelationship(embeddingRule, this).create();
    }

    protected static class CreateFunction
        implements Function<UUID, Promise<Boolean>> {
        private RHS rhs;
        public CreateFunction(RHS rhs) {
            this.rhs = rhs;
        }
        public Promise<Boolean> apply(UUID ruleUUID) {
            byte[] bytes = ruleUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            UUID uuid = UUID.nameUUIDFromBytes(bytes);
            this.rhs.jsonProperties.put("uuid", uuid.toString());
            return RHSManager.create(this.rhs);
        }
    }

}
