package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.AVMManager;


public abstract class AVM extends LabeledNodeWithProperties {
    public Rule rule;
    public JsonNode json;

    protected AVM() {
        this.label = NodeType.AVM;
        this.jsonProperties = Json.newObject();
    }

    public AVM(Rule rule) {
        this();
        this.rule = rule;
    }

    public abstract Promise<UUID> getUUID();

    public abstract Promise<Boolean> create();

    protected class UUIDFunction implements Function<UUID, UUID> {
        public UUID apply(UUID parentUUID) {
            byte[] bytes = parentUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            return UUID.nameUUIDFromBytes(bytes);
        }
    }

    protected class CreateFunction implements
                                     Function<UUID, Promise<Boolean>> {
        private AVM avm;
        public CreateFunction(AVM avm) {
            this.avm = avm;
        }
        public Promise<Boolean> apply(UUID parentUUID) {
            byte[] bytes = parentUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            UUID uuid = UUID.nameUUIDFromBytes(bytes);
            this.avm.jsonProperties.put("uuid", uuid.toString());
            return AVMManager.create(this.avm);
        }
    }

}
