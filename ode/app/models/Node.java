package models;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;


import neo4play.Neo4jService;


public class Node {
    protected Neo4jService connector = new Neo4jService();

    public boolean persisted = false;
    public String label;

    public Node(String label) {
        this.label = label;
    }

    public Promise<Node> create() {
        String query = "CREATE (n:" + this.label + ") RETURN n";
        ObjectNode json = Json.newObject();
        json.put("query", query);
        Promise<WS.Response> response = this.connector.post("/cypher", json);
        return response.map(new SaveFunction(this));
    }

    protected class SaveFunction implements Function<WS.Response, Node> {
        private Node node;

        public SaveFunction(Node node) {
            this.node = node;
        }

        public Node apply(WS.Response response) {
            if (response.getStatus() == Status.OK) {
                this.node.persisted = true;
                return this.node;
            }
            return null;
        }
    }
}
