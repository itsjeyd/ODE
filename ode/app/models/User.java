package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import neo4play.Neo4jService;


public class User {
    private Neo4jService dbService = new Neo4jService();
    private String label = "User";

    public String username;
    public String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Promise<User> create() {
        ObjectNode props = Json.newObject();
        props.put("username", this.username);
        props.put("password", this.password);
        Promise<WS.Response> response = dbService
            .createLabeledNodeWithProperties(this.label, props);
        return response.map(new CreatedFunction(this));
    }

    public Promise<Boolean> exists() {
        ObjectNode props = Json.newObject();
        props.put("username", this.username);
        Promise<WS.Response> response = dbService
            .getLabeledNodeWithProperties(this.label, props);
        return response.map(new ExistsFunction());
    }

    public Promise<User> get() {
        ObjectNode props = Json.newObject();
        props.put("username", this.username);
        props.put("password", this.password);
        Promise<WS.Response> response = dbService
            .getLabeledNodeWithProperties(this.label, props);
        return response.map(new GetFunction(this));
    }

    public Promise<User> delete() {
        ObjectNode props = Json.newObject();
        props.put("username", this.username);
        Promise<WS.Response> response = dbService
            .deleteLabeledNodeWithProperties(this.label, props);
        return response.map(new DeleteFunction(this));
    }

    private class CreatedFunction implements Function<WS.Response, User> {
        private User user;
        public CreatedFunction(User user) {
            this.user = user;
        }
        public User apply(WS.Response response) {
            if (response.getStatus() == Status.OK) {
                return this.user;
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

    private class GetFunction implements Function<WS.Response, User> {
        private User user;
        public GetFunction(User user) {
            this.user = user;
        }
        public User apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get("data").size() > 0) {
                return this.user;
            }
            return null;
        }
    }

    private class DeleteFunction implements Function<WS.Response, User> {
        private User user;
        public DeleteFunction(User user) {
            this.user = user;
        }
        public User apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get("data").size() == 0) {
                return null;
            }
            return this.user;
        }
    }
}
