package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import neo4play.Neo4jService;


public class Feature {
    private Neo4jService dbService = new Neo4jService();
    private String label = "Feature";

    public String name;
    public String type;

    public Feature(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Promise<Feature> create() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        props.put("type", this.type);
        Promise<WS.Response> response = dbService
            .createLabeledNodeWithProperties(this.label, props);
        return response.map(new CreatedFunction(this));
    }

    public Promise<Boolean> exists() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        Promise<WS.Response> response = dbService
            .getLabeledNodeWithProperties(this.label, props);
        return response.map(new ExistsFunction());
    }

    public Promise<Feature> delete() {
        ObjectNode props = Json.newObject();
        props.put("name", this.name);
        Promise<WS.Response> response = dbService
            .deleteLabeledNodeWithProperties(this.label, props);
        return response.map(new DeleteFunction(this));
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

}
