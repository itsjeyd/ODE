package models.nodes;

import constants.NodeType;
import managers.nodes.OutputStringManager;


public class OutputString extends LabeledNodeWithProperties {

    public static final OutputStringManager nodes =
        new OutputStringManager();

    private String content;

    private OutputString() {
        super(NodeType.OUTPUT_STRING);
    }

    public OutputString(String uuid) {
        this();
        this.jsonProperties.put("uuid", uuid);
    }

    public OutputString(String uuid, String content) {
        this(uuid);
        this.content = content;
        this.jsonProperties.put("content", content);
    }

}
