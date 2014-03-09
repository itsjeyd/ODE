package models.nodes;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import models.relationships.HasFeatureRelationship;
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
        Promise<JsonNode> json = this.toJSON();
        final LHS lhs = this;
        return json.map(
            new Function<JsonNode, LHS>() {
                public LHS apply(JsonNode json) {
                    lhs.json = json;
                    return lhs;
                }
            });
    }

    public Promise<Boolean> add(final Feature feature) {
        final LHS lhs = this;
        Promise<UUID> uuid = this.getUUID();
        Promise<Boolean> connected = uuid.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID uuid) {
                    lhs.jsonProperties.put("uuid", uuid.toString());
                    return new HasFeatureRelationship(lhs, feature).create();
                }
            });
        return connected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean connected) {
                    if (connected) {
                        return feature.addDefaultValue(lhs.rule, lhs);
                    }
                    return Promise.pure(false);
                }
            });
    }

}
