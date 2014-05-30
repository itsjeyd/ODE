package models.nodes;

import java.nio.charset.Charset;
import java.util.UUID;
import managers.nodes.LHSManager;
import play.libs.F.Function;
import play.libs.F.Promise;


public class LHS extends AVM {

    public static final LHSManager nodes = new LHSManager();

    public Rule parent;

    public LHS(String uuid) {
        this.jsonProperties.put("uuid", uuid);
    }

    public LHS(Rule rule) {
        super(rule);
        this.parent = rule;
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.map(new UUIDFunction());
    }

    protected static class UUIDFunction implements Function<UUID, UUID> {
        public UUID apply(UUID parentUUID) {
            byte[] bytes = parentUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            return UUID.nameUUIDFromBytes(bytes);
        }
    }

}
