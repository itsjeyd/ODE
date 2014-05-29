package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;
import java.util.UUID;
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

}
