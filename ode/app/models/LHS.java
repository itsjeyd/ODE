package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.LHSManager;


public class LHS extends LabeledNodeWithProperties {
    public Rule rule;
    public JsonNode json;

    private LHS() {
        this.label = NodeType.AVM;
        this.jsonProperties = Json.newObject();
    }

    public LHS(Rule rule) {
        this();
        this.rule = rule;
        UUID uuid = UUID.nameUUIDFromBytes(
            rule.name.getBytes(Charset.forName("UTF-8")));
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public Promise<Boolean> create() {
        Promise<Boolean> created = this.exists()
            .flatMap(new CreateFunction(this));
        return created.flatMap(new ConnectToRuleFunction(this));
    }

    public Promise<LHS> get() {
        Promise<JsonNode> json = LHSManager.get(this);
        return json.flatMap(new GetFunction(this));
    }

    public Promise<Boolean> add(Feature feature) {
        return new HasRelationship(this, feature).create();
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private LHS lhs;
        public CreateFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return LHSManager.create(this.lhs);
        }
    }

    private class ConnectToRuleFunction
        implements Function<Boolean, Promise<Boolean>> {
        private LHS lhs;
        public ConnectToRuleFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<Boolean> apply(Boolean created) {
            return new LHSRelationship(this.lhs).create();
        }
    }

    private class GetFunction implements Function<JsonNode, Promise<LHS>> {
        private LHS lhs;
        public GetFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<LHS> apply(JsonNode json) {
            JsonNode data = json.get("data");
            if (data.size() > 0) {
                // Process node-relationship-node triples appropriately;
                // for now just get names of end nodes (i.e.,
                // features) and add them to pairs with arbitrary
                // default value
                List<JsonNode> endNodes = data.findValues("end");
                List<Promise<? extends Feature>> featureList =
                    new ArrayList<Promise<? extends Feature>>();
                for (JsonNode endNode: endNodes) {
                    Promise<Feature> feature = Feature
                        .getByURL(endNode.asText());
                    featureList.add(feature);
                }
                Promise<List<Feature>> features = Promise
                    .sequence(featureList);
                Promise<ObjectNode> pairs = features.map(
                    new Function<List<Feature>, ObjectNode>() {
                        public ObjectNode apply(List<Feature> features) {
                            ObjectNode pairs = Json.newObject();
                            for (Feature feature: features) {
                                ObjectNode value = Json.newObject();
                                pairs.put(feature.name, value);
                            }
                            return pairs;
                        }
                    });
                return pairs.map(new SetPairsFunction(this.lhs));
            } else {
                ObjectNode pairs = Json.newObject();
                this.lhs.json = pairs;
                return Promise.pure(this.lhs);
            }
        }
    }

    private static class SetPairsFunction implements
                                            Function<ObjectNode, LHS> {
        private LHS lhs;
        public SetPairsFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public LHS apply(ObjectNode pairs) {
            this.lhs.json = pairs;
            return this.lhs;
        }
    }

}
