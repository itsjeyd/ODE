package managers.nodes;

import play.libs.F.Promise;

import models.nodes.User;


public class UserManager extends LabeledNodeWithPropertiesManager {

    public static Promise<Boolean> create(User user) {
        user.jsonProperties.put("password", user.password);
        return LabeledNodeWithPropertiesManager.create(user);
    }

}
