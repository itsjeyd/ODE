package models.nodes;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.PartManager;


public class Part extends LabeledNodeWithProperties {

    public static final PartManager nodes = new PartManager();

    public String content;

    private Part() {
        super(NodeType.PART);
    }

    public Part(UUID uuid) {
        this();
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public Part(String content) {
        this();
        this.content = content;
        this.jsonProperties.put("content", content);
    }

    public Part(String uuid, String content) {
        this();
        this.jsonProperties.put("uuid", uuid);
        this.jsonProperties.put("content", content);
        this.content = content;
    }

    protected JsonNode toJSON() {
        return this.jsonProperties.deepCopy();
    }

    public Promise<UUID> getUUID() {
        return this.exists().flatMap(
            new Function<Boolean, Promise<UUID>>() {
                public Promise<UUID> apply(Boolean exists) {
                    if (exists) {
                        return PartManager.getUUID(Part.this);
                    }
                    return Promise.pure(UUID.randomUUID());
                }
            });
    }

}
