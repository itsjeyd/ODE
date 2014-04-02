package models.nodes;

import play.libs.F.Function;
import play.libs.F.None;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Some;

import constants.NodeType;
import managers.nodes.UserManager;


public class User extends LabeledNodeWithProperties {
    public String username;
    public String password;

    private User() {
        super(NodeType.USER);
    }

    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
        this.jsonProperties.put("username", username);
    }

    public Promise<Option<User>> get() {
        return this.exists().map(new GetFunction(this));
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    public Promise<Boolean> delete() {
        return Promise.pure(false);
    }

    private class GetFunction implements Function<Boolean, Option<User>> {
        private User user;
        public GetFunction(User user) {
            this.user = user;
        }
        public Option<User> apply(Boolean exists) {
            if (exists) {
                return new Some<User>(this.user);
            }
            return new None<User>();
        }
    }

    private class CreateFunction
        implements Function<Boolean, Promise<Boolean>> {
        private User user;
        public CreateFunction(User user) {
            this.user = user;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return UserManager.create(this.user);
        }
    }

}
