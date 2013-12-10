package models;

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
}
