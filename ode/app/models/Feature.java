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

}