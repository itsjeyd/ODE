package managers;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.NodeCreatedFunction;
import models.User;
import neo4play.Neo4jService;


public class UserManager {

    public static Promise<Boolean> create(User user) {
        user.jsonProperties.put("password", user.password);
        Promise<WS.Response> response = Neo4jService
            .createLabeledNodeWithProperties(
                user.label.toString(), user.jsonProperties);
        return response.map(new NodeCreatedFunction());
    }

}
