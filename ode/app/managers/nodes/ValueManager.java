package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;
import java.util.ArrayList;
import java.util.List;
import models.nodes.Value;
import models.relationships.Allows;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;


public class ValueManager extends LabeledNodeWithPropertiesManager {

    public ValueManager() {
        this.label = NodeType.VALUE.toString();
    }

    // READ

    public Promise<List<Value>> all() {
        Promise<List<JsonNode>> json = all(this.label);
        return json.map(
            new Function<List<JsonNode>, List<Value>>() {
                public List<Value> apply(List<JsonNode> json) {
                    List<Value> values = new ArrayList<Value>();
                    for (JsonNode node: json) {
                        String name = node.get("name").asText();
                        if (!name.equals("underspecified")) {
                            values.add(new Value(name));
                        }
                    }
                    return values;
                }
            });
    }

    // DELETE

    public void delete() {
        Promise<List<Value>> values = all();
        values.onRedeem(
            new Callback<List<Value>>() {
                public void invoke(List<Value> values) {
                    for (Value value: values) {
                        final JsonNode props = value.getProperties();
                        Promise<Boolean> orphaned = orphaned(props);
                        orphaned.onRedeem(
                            new Callback<Boolean>() {
                                public void invoke(Boolean orphaned) {
                                    if (orphaned) {
                                        delete(props);
                                    }
                                }
                            });
                    }

                }
            });
    }

    private Promise<Boolean> orphaned(JsonNode properties) {
        Value value = new Value(properties.get("name").asText());
        return super.orphaned(value, Allows.relationships);
    }

}
