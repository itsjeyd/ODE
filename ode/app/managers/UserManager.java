package managers;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.NodeCreatedFunction;
import models.User;
import neo4play.Neo4jService;


public class UserManager {

    private static Neo4jService dbService = new Neo4jService();

    public static Promise<Boolean> create(User user) {
        user.jsonProperties.put("password", user.password);
        Promise<WS.Response> response = dbService
            .createLabeledNodeWithProperties(
                user.label.toString(), user.jsonProperties);
        return response.map(new NodeCreatedFunction());
    }

}
