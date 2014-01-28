package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.ValueManager;


public class Value extends OntologyNode {
    private Value() {
        this.label = NodeType.VALUE;
        this.jsonProperties = Json.newObject();
    }

    public Value(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public static Promise<List<Value>> all() {
        Promise<List<JsonNode>> json = ValueManager.all();
        return json.map(new AllFunction());
    }

    public Promise<Boolean> updateName(String newName) {
        return ValueManager.updateName(this, newName);
    }

    public Promise<Boolean> delete() {
        return ValueManager.delete(this);
    }

    public Promise<Boolean> isOrphan() {
        Promise<JsonNode> json = ValueManager.getIncomingRelationships(this);
        return json.map(new IsOrphanFunction());
    }

    public Promise<Boolean> deleteIfOrphaned() {
        return this.isOrphan().flatMap(new DeleteIfOrphanedFunction(this));
    }

    public static void deleteOrphans() {
        Promise<List<Value>> values = Value.all();
        values.onRedeem(new Callback<List<Value>>() {
                public void invoke(List<Value> values) {
                    for (Value value: values) {
                        value.deleteIfOrphaned();
                    }
                }
            });
    }


    private class IsOrphanFunction implements Function<JsonNode, Boolean> {
        public Boolean apply(JsonNode json) {
            return json.size() == 0;
        }
    }

    private class DeleteIfOrphanedFunction
        implements Function<Boolean, Promise<Boolean>> {
        private Value value;
        public DeleteIfOrphanedFunction(Value value) {
            this.value = value;
        }
        public Promise<Boolean> apply(Boolean isOrphan) {
            if (isOrphan) {
                return value.delete();
            }
            else {
                return Promise.pure(false);
            }
        }
    }

    private static class AllFunction
        implements Function<List<JsonNode>, List<Value>> {
        public List<Value> apply(List<JsonNode> dataNodes) {
            List<Value> values = new ArrayList<Value>();
            for (JsonNode dataNode: dataNodes) {
                String name = dataNode.get("name").asText();
                values.add(new Value(name));
            }
            Collections.sort(
                values, new Comparator<Value>() {
                    public int compare(Value v1, Value v2) {
                        return v1.name.compareTo(v2.name);
                    }
                });
            return values;
        }
    }

}
