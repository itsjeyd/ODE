package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import play.libs.F.Function;
import play.libs.F.Promise;

import managers.nodes.AVMManager;
import models.relationships.HasSubstructureRelationship;


public class Substructure extends AVM {
    public AVM parent;
    public Feature embeddingFeature;

    public Substructure(Rule rule, AVM parent, Feature embeddingFeature) {
        super(rule);
        this.parent = parent;
        this.embeddingFeature = embeddingFeature;
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.map(new UUIDFunction(this.embeddingFeature));
    }

    public Promise<Boolean> create() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.flatMap(new CreateFunction(this));
    }

    public Promise<Boolean> connectTo(Feature embeddingFeature) {
        return new HasSubstructureRelationship(embeddingFeature, this)
            .create();
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

    protected static class CreateFunction
        implements Function<UUID, Promise<Boolean>> {
        private Substructure substructure;
        public CreateFunction(Substructure substructure) {
            this.substructure = substructure;
        }
        public Promise<Boolean> apply(UUID parentUUID) {
            byte[] bytes = (parentUUID.toString() +
                            substructure.embeddingFeature.name)
                .getBytes(Charset.forName("UTF-8"));
            UUID uuid = UUID.nameUUIDFromBytes(bytes);
            this.substructure.jsonProperties.put("uuid", uuid.toString());
            return AVMManager.create(this.substructure);
        }
    }

}
