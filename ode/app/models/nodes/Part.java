package models.nodes;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.PartManager;
import models.relationships.HasPartRelationship;


public class Part extends LabeledNodeWithProperties {

    public static final PartManager nodes = new PartManager();

    public String content;

    private Part() {
        super(NodeType.PART);
    }

    private Part(UUID uuid) {
        this();
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public Part(String content) {
        this();
        this.content = content;
        this.jsonProperties.put("content", content);
    }

    public Part(String uuid, String content) {
        this(UUID.fromString(uuid), content);
    }

    public static Part of(UUID uuid) {
        return new Part(uuid);
    }

    public static Part of(String content) {
        return new Part(content);
    }

    private Part(UUID uuid, String content) {
        this();
        this.jsonProperties.put("uuid", uuid.toString());
        this.jsonProperties.put("content", content);
        this.content = content;
    }

    public static Part of(UUID uuid, String content) {
        return new Part(uuid, content);
    }

    protected JsonNode toJSON() {
        return this.jsonProperties.deepCopy();
    }

    public Promise<Boolean> isOrphan() {
        return PartManager.isOrphan(this);
    }

    public Promise<UUID> getUUID() {
        return this.exists().flatMap(
            new Function<Boolean, Promise<UUID>>() {
                public Promise<UUID> apply(Boolean exists) {
                    if (exists) {
                        return PartManager.getUUID(Part.this);
                    }
                    return Promise.pure(UUID.randomUUID());
                }
            });
    }

    public Promise<Boolean> create() {
        return PartManager.create(this);
    }

    public Promise<Boolean> connectTo(final Slot slot) {
        final Part part = new Part(this.content);
        return part.exists().flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return new HasPartRelationship(slot, part).create();
                    }
                    Promise<Boolean> created = Part.this.create();
                    return created.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean created) {
                                if (created) {
                                    return new HasPartRelationship(
                                        slot, Part.this).create();
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
    }

    public Promise<Boolean> removeFrom(Slot slot) {
        Promise<Boolean> disconnected = HasPartRelationship
            .delete(slot, this);
        return disconnected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean disconnected) {
                    if (disconnected) {
                        return Part.this.deleteIfOrphaned();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> delete() {
        return PartManager.delete(this);
    }

    public Promise<Boolean> deleteIfOrphaned() {
        return this.isOrphan().flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean isOrphan) {
                    if (isOrphan) {
                        return Part.this.delete();
                    }
                    return Promise.pure(true);
                }
            });
    }

}
