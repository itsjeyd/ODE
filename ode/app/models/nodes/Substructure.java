package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import play.libs.F.Function;
import play.libs.F.Promise;

import managers.nodes.AVMManager;


public class Substructure extends AVM {

    public static final AVMManager nodes = new AVMManager();

    public AVM parent;
    public Feature embeddingFeature;

    protected Substructure(Rule rule, UUID uuid) {
        super(rule);
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public Substructure(Rule rule, AVM parent, Feature embeddingFeature) {
        super(rule);
        this.parent = parent;
        this.embeddingFeature = embeddingFeature;
    }

    public Substructure(String uuid) {
        this.jsonProperties.put("uuid", uuid);
    }

    public Promise<UUID> getUUID() {
        if (this.jsonProperties.has("uuid")) {
            UUID uuid = UUID.fromString(
                this.jsonProperties.get("uuid").asText());
            return Promise.pure(uuid);
        }
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.map(new UUIDFunction(this.embeddingFeature));
    }

    private static class UUIDFunction implements Function<UUID, UUID> {
        private Feature embeddingFeature;
        public UUIDFunction(Feature embeddingFeature) {
            this.embeddingFeature = embeddingFeature;
        }
        public UUID apply(UUID parentUUID) {
            byte[] bytes = (parentUUID.toString() + embeddingFeature.name)
                .getBytes(Charset.forName("UTF-8"));
            return UUID.nameUUIDFromBytes(bytes);
        }
    }

}
