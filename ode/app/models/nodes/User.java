package models.nodes;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.None;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Some;
import play.libs.F.Tuple;

import constants.NodeType;
import managers.UserManager;


public class User extends LabeledNodeWithProperties {
    public String username;
    public String password;

    private User() {
        this.label = NodeType.USER;
        this.jsonProperties = Json.newObject();
    }

    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
        this.jsonProperties.put("username", username);
    }

    public Promise<Tuple<Option<User>, Boolean>> getOrCreate() {
        return this.exists().flatMap(new GetOrCreateFunction(this));
    }

    public Promise<Option<User>> get() {
        return this.exists().map(new GetFunction(this));
    }


    private class GetOrCreateFunction
        implements Function<Boolean, Promise<Tuple<Option<User>, Boolean>>> {
        private User user;
        public GetOrCreateFunction(User user) {
            this.user = user;
        }
        public Promise<Tuple<Option<User>, Boolean>> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(
                    new Tuple<Option<User>, Boolean>(
                        new Some<User>(this.user), false));
            }
            Promise<Boolean> created = UserManager.create(this.user);
            return created.map(new CreatedFunction(this.user));
        }
    }

    private class CreatedFunction
        implements Function<Boolean, Tuple<Option<User>, Boolean>> {
        private User user;
        public CreatedFunction(User user) {
            this.user = user;
        }
        public Tuple<Option<User>, Boolean> apply(Boolean created) {
            if (created) {
                return new Tuple<Option<User>, Boolean>(
                    new Some<User>(this.user), true);
            }
            return new Tuple<Option<User>, Boolean>(
                new None<User>(), false);
        }
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

}
