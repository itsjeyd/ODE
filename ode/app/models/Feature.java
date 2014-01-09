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


public class Feature extends Model {
    private static Neo4jService dbService = new Neo4jService();

    public String name;
    public String featureType;
    public String description;
    public List<String> values; // "Values" can be other features or
                                // atomic values

    private Feature() {
        this.label = "Feature";
        this.jsonProperties = Json.newObject();
    }

    public Feature(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Feature(String name, String type) {
        this(name);
        this.featureType = type;
    }

    public Feature(String name, String type, String description) {
        this(name);
        this.featureType = type;
        this.description = description;
    }

    @Override
    public Promise<Feature> create() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        props.put("type", this.featureType);
        props.put("description", this.description);
        Promise<WS.Response> response = dbService
            .createLabeledNodeWithProperties(label, props);
        return response.map(new CreatedFunction<Feature>(this));
    }

    @Override
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
        return response.map(new DeletedFunction<Feature>(this));
    }

    public static Promise<List<Feature>> all() {
        Promise<WS.Response> response = dbService.getNodesByLabel("Feature");
        return response.map(new AllFunction());
    }

    public Promise<Feature> update() {
        final ObjectNode oldProps = Json.newObject();
        oldProps.put("name", this.name);
        Promise<String> nodeURL = dbService.getNodeURL(label, oldProps);
        Promise<String> description = nodeURL.flatMap(
            new Function<String, Promise<String>>() {
                public Promise<String> apply(String nodeURL) {
                    return dbService.getNodeProperty(nodeURL, "description");
        }});
        final String newType = this.featureType;
        Promise<WS.Response> response = description.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String description) {
                    oldProps.put("description", description);
                    ObjectNode newProps = Json.newObject();
                    newProps.put("name", oldProps.get("name"));
                    newProps.put("type", newType);
                    newProps.put("description", description);
                    return dbService
                        .updateNodeProperties(label, oldProps, newProps);
        }});
        return response.map(new UpdateFunction(this));
    }

    public Promise<Relationship> connectTo(
        Model target, String relationshipType) {
        Relationship allowsRelationship = new Relationship(
            "ALLOWS", this, target);
        return allowsRelationship.create();
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

    public Promise<Feature> setType(String newType) {
        ObjectNode oldProps = this.jsonProperties;
        ObjectNode newProps = Json.newObject();
        newProps.put("name", this.name);
        newProps.put("type", newType);
        newProps.put("description", this.description);
        Promise<WS.Response> response = dbService.updateNodeProperties(
            label, oldProps, newProps);
        this.jsonProperties = newProps;
        return response.map(new UpdateFunction(this));
    }

    public Promise<Feature> get() {
        Promise<WS.Response> response = dbService
            .getLabeledNodeWithProperties(this.label, this.jsonProperties);
        return response.map(new GetFunction(this));
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
                JsonNode descriptionNode = dataNode.get("description");
                if (descriptionNode != null) {
                    String description = descriptionNode.asText();
                    features.add(new Feature(name, type, description));
                } else {
                    features.add(new Feature(name, type));
                }
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

    private class ValueURLsFunction implements
        Function<WS.Response, List<String>> {
        public List<String> apply(WS.Response response) {
            JsonNode json = response.asJson();
            return json.findValuesAsText("end");
        }
    }

    private class GetFunction implements Function<WS.Response, Feature> {
        private Feature feature;
        public GetFunction(Feature feature) {
            this.feature = feature;
        }
        public Feature apply(WS.Response response) {
            JsonNode json = response.asJson();
            System.out.println("Response (JSON): " + json.toString());

            if (json.get("data").size() == 1) {
                this.feature.featureType = json.findValue("type").asText();
                this.feature.description = json.findValue("description")
                    .asText();
                return this.feature;
            }
            return null;
        }
    }

}
