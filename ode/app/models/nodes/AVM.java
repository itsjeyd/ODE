package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.AVMManager;
import models.relationships.HasSubstructureRelationship;


public class AVM extends LabeledNodeWithProperties {
    public Rule rule;
    public LHS parentAVM;
    public Feature embeddingFeature;

    private AVM() {
        this.label = NodeType.AVM;
        this.jsonProperties = Json.newObject();
    }

    public AVM(Rule rule, LHS parentAVM, Feature embeddingFeature) {
        this();
        this.rule = rule;
        this.parentAVM = parentAVM;
        this.embeddingFeature = embeddingFeature;
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> ruleUUID = this.parentAVM.getUUID();
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
        Promise<UUID> lhsUUID = this.parentAVM.getUUID();
        Promise<Boolean> created = lhsUUID
            .flatMap(new CreateFunction(this));
        return created.flatMap(new ConnectToFeatureFunction(this));
    }

    private class CreateFunction implements
                                     Function<UUID, Promise<Boolean>> {
        private AVM avm;
        public CreateFunction(AVM avm) {
            this.avm = avm;
        }
        public Promise<Boolean> apply(UUID lhsUUID) {
            byte[] bytes = lhsUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            UUID uuid = UUID.nameUUIDFromBytes(bytes);
            this.avm.jsonProperties.put("uuid", uuid.toString());
            return AVMManager.create(this.avm);
        }
    }

    private class ConnectToFeatureFunction
        implements Function<Boolean, Promise<Boolean>> {
        private AVM avm;
        public ConnectToFeatureFunction(AVM avm) {
            this.avm = avm;
        }
        public Promise<Boolean> apply(Boolean created) {
            return new HasSubstructureRelationship(this.avm).create();
        }
    }

}
