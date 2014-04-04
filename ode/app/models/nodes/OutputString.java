package models.nodes;

import java.util.UUID;

import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.None;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Some;

import constants.NodeType;
import managers.nodes.OutputStringManager;
import models.relationships.HasStringRelationship;


public class OutputString extends LabeledNodeWithProperties {
    private String content;

    private OutputString() {
        super(NodeType.OUTPUT_STRING);
    }

    private OutputString(UUID uuid) {
        this();
        this.jsonProperties.put("uuid", uuid.toString());
    }

    private OutputString(String content) {
        this();
        this.content = content;
        this.jsonProperties.put("content", content);
    }

    private OutputString(UUID uuid, String content) {
        this(content);
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public static OutputString of(UUID uuid) {
        return new OutputString(uuid);
    }

    public static OutputString of(String content) {
        return new OutputString(content);
    }

    public static OutputString of(UUID uuid, String content) {
        return new OutputString(uuid, content);
    }

    public Promise<Boolean> isOrphan() {
        return OutputStringManager.isOrphan(this);
    }

    public Promise<UUID> getUUID() {
        return this.exists().flatMap(
            new Function<Boolean, Promise<UUID>>() {
                public Promise<UUID> apply(Boolean exists) {
                    if (exists) {
                        return OutputStringManager.getUUID(OutputString.this);
                    }
                    return Promise.pure(UUID.randomUUID());
                }
            });
    }

    public Promise<Boolean> create() {
        return OutputStringManager.create(this);
    }

    public Promise<Boolean> connectTo(final CombinationGroup group) {
        final OutputString outputString = new OutputString(this.content);
        return outputString.exists().flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return new HasStringRelationship(
                            group, outputString).create();
                    }
                    Promise<Boolean> created = OutputString.this.create();
                    return created.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean created) {
                                if (created) {
                                    return new HasStringRelationship(
                                        group, OutputString.this).create();
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
    }

    public Promise<Boolean> removeFrom(CombinationGroup group) {
        Promise<Boolean> disconnected = HasStringRelationship
            .delete(group, this);
        return disconnected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean disconnected) {
                    if (disconnected) {
                        return OutputString.this.deleteIfOrphaned();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> delete() {
        return OutputStringManager.delete(this);
    }

    public Promise<Boolean> deleteIfOrphaned() {
        return this.isOrphan().flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean isOrphan) {
                    if (isOrphan) {
                        return OutputString.this.delete();
                    }
                    return Promise.pure(true);
                }
            });
    }

}
