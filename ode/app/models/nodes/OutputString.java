package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.NodeType;
import java.util.UUID;
import managers.nodes.OutputStringManager;
import play.libs.F.Function;
import play.libs.F.Promise;


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

    protected JsonNode toJSON() {
        ObjectNode json = this.jsonProperties.deepCopy();
        ArrayNode tokens = JsonNodeFactory.instance.arrayNode();
        String[] contentTokens = this.content.split(" ");
        for (String token: contentTokens) {
            tokens.add(token);
        }
        json.put("tokens", tokens);
        return json;
    }

}
