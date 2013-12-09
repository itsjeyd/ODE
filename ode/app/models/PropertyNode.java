package models;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Promise;

import models.Node;


public class PropertyNode extends Node {
    public Map<String, String> properties;

    public PropertyNode(String label) {
        super(label);
    }

    public PropertyNode(String label, Map<String, String> properties) {
        super(label);
        this.properties = properties;
    }

    public Promise<Node> create() {
        String query = "CREATE (n:" + this.label + " { props }) RETURN n";
        ObjectNode json = Json.newObject();
        ObjectNode params = Json.newObject();
        ObjectNode props = Json.newObject();
        for (Map.Entry<String, String> property: properties.entrySet()) {
            props.put(property.getKey(), property.getValue());
        }
        params.put("props", props);
        json.put("query", query);
        json.put("params", params);
        Promise<WS.Response> response = this.connector.post("/cypher", json);
        return response.map(new SaveFunction(this));
    }
}
