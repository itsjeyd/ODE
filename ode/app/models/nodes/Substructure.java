package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import play.libs.F.Function;
import play.libs.F.Promise;

import models.relationships.HasSubstructureRelationship;


public class Substructure extends AVM {
    public AVM parent;

    public Substructure(Rule rule, AVM parent) {
        super(rule);
        this.parent = parent;
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.map(new UUIDFunction());
    }

    public Promise<Boolean> create() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.flatMap(new CreateFunction(this));
    }

    public Promise<Boolean> connectTo(Feature embeddingFeature) {
        return new HasSubstructureRelationship(embeddingFeature, this)
            .create();
    }

}
