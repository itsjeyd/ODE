package models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import neo4play.Neo4jService;


public class Feature {
    private static Neo4jService dbService = new Neo4jService();
    private static String label = "Feature";

    public String name;
    public String featureType;
    public List<String> values; // "Values" can be other features or
                                // atomic values

    public Feature(String name) {
        this.name = name;
    }

    public Feature(String name, String type) {
        this.name = name;
        this.featureType = type;
    }

    public Promise<Feature> create() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        props.put("type", this.featureType);
        Promise<WS.Response> response = dbService
            .createLabeledNodeWithProperties(label, props);
        return response.map(new CreatedFunction(this));
    }

    public Promise<Boolean> exists() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        Promise<WS.Response> response = dbService
            .getLabeledNodeWithProperties(label, props);
        return response.map(new ExistsFunction());
    }

    public Promise<Feature> delete() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        Promise<WS.Response> response = dbService
            .deleteLabeledNodeWithProperties(label, props);
        return response.map(new DeleteFunction(this));
    }

    public static Promise<List<Feature>> all() {
        Promise<WS.Response> response = dbService.getNodesByLabel(label);
        return response.map(new AllFunction());
    }

    public Promise<Feature> update() {
        ObjectNode oldProps = Json.newObject();
        oldProps.put("name", this.name);
        ObjectNode newProps = Json.newObject();
        newProps.put("name", this.name);
        newProps.put("type", this.featureType);
        Promise<WS.Response> response = dbService
            .updateNodeProperties(label, oldProps, newProps);
        return response.map(new UpdateFunction(this));
    }

    public Promise<Feature> connectTo(Feature otherFeature,
                                      String relationshipType) {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        ObjectNode otherProps = Json.newObject();
        otherProps.put("name", otherFeature.name);
        Promise<WS.Response> response = dbService
            .createRelationship(
                label, props, label, otherProps, relationshipType);
        return response.map(new RelationshipCreatedFunction(this));
    }

    public Promise<Feature> connectTo(Value value, String relationshipType) {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        ObjectNode valueProps = Json.newObject();
        valueProps.put("name", value.name);
        Promise<WS.Response> response = dbService
            .createRelationship(
                label, props, Value.label, valueProps, relationshipType);
        return response.map(new RelationshipCreatedFunction(this));
    }

    public Promise<List<String>> getValues() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        Promise<WS.Response> response = dbService
            .getOutgoingRelationshipsByType(label, props, "ALLOWS");
        final Promise<List<String>> valueURLs = response.map(
            new ValueURLsFunction());
        Promise<List<String>> values = valueURLs.map(
            new Function<List<String>, List<String>>() {
                public List<String> apply(List<String> urls) {
                    List<String> values = new ArrayList<String>();
                    for (String url: urls) {
                        Promise<String> value = dbService
                            .getNodeProperty(url, "name");
                        values.add(value.get());
                    }
                    return values;
        }});
        return values;
    }

    private class CreatedFunction implements Function<WS.Response, Feature> {
        private Feature feature;
        public CreatedFunction(Feature feature) {
            this.feature = feature;
        }
        public Feature apply(WS.Response response) {
            if (response.getStatus() == Status.OK) {
                return this.feature;
            }
            return null;
        }
    }

    private class ExistsFunction implements Function<WS.Response, Boolean> {
        public Boolean apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get("data").size() > 0) {
                return true;
            }
            return false;
        }
    }

    private class DeleteFunction implements Function<WS.Response, Feature> {
        private Feature feature;
        public DeleteFunction(Feature feature) {
            this.feature = feature;
        }
        public Feature apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get("data").size() == 0) {
                return null;
            }
            return this.feature;
        }
    }

    private static class AllFunction implements
                                         Function<WS.Response, List<Feature>>
    {
        public List<Feature> apply(WS.Response response) {
            List<Feature> features = new ArrayList<Feature>();
            JsonNode json = response.asJson();
            List<JsonNode> dataNodes = json.findValues("data");
            for (JsonNode dataNode: dataNodes) {
                String name = dataNode.get("name").asText();
                String type = dataNode.get("type").asText();
                features.add(new Feature(name, type));
            }
            return features;
        }
    }

    private class UpdateFunction implements Function<WS.Response, Feature> {
        private Feature feature;
        public UpdateFunction(Feature feature) {
            this.feature = feature;
        }
        public Feature apply(WS.Response response) {
            if (response.getStatus() == Status.NO_CONTENT) {
                return this.feature;
            }
            return null;
        }
    }

    private class RelationshipCreatedFunction
        implements Function<WS.Response, Feature> {
        private Feature feature;
        public RelationshipCreatedFunction(Feature feature) {
            this.feature = feature;
        }
        public Feature apply(WS.Response response) {
            if (response.getStatus() == Status.CREATED) {
                return this.feature;
            }
            return null;
        }
    }

    private class ValueURLsFunction implements
        Function<WS.Response, List<String>> {
        public List<String> apply(WS.Response response) {
            JsonNode json = response.asJson();
            return json.findValuesAsText("end");
        }
    }

}
