package managers;

import play.libs.F.Promise;

import models.User;


public class UserManager extends LabeledNodeWithPropertiesManager {

    public static Promise<Boolean> create(User user) {
        user.jsonProperties.put("password", user.password);
        return LabeledNodeWithPropertiesManager.create(user);
    }

}
