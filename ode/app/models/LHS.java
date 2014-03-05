package models;

import java.util.UUID;
import java.nio.charset.Charset;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.LHSManager;


public class LHS extends LabeledNodeWithProperties {
    public Rule rule;

    private LHS() {
        this.label = NodeType.AVM;
        this.jsonProperties = Json.newObject();
    }

    public LHS(Rule rule) {
        this();
        this.rule = rule;
        UUID uuid = UUID.nameUUIDFromBytes(
            rule.name.getBytes(Charset.forName("UTF-8")));
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public Promise<Boolean> create() {
        Promise<Boolean> created = this.exists()
            .flatMap(new CreateFunction(this));
        return created.flatMap(new ConnectToRuleFunction(this));
    }

    public Promise<Boolean> add(Feature feature) {
        return new HasRelationship(this, feature).create();
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private LHS lhs;
        public CreateFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return LHSManager.create(this.lhs);
        }
    }

    private class ConnectToRuleFunction
        implements Function<Boolean, Promise<Boolean>> {
        private LHS lhs;
        public ConnectToRuleFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<Boolean> apply(Boolean created) {
            return new LHSRelationship(this.lhs).create();
        }
    }

}
