package models.nodes;

import java.util.UUID;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.OutputStringManager;
import models.relationships.HasStringRelationship;


public class OutputString extends LabeledNodeWithProperties {
    private String content;

    private OutputString() {
        super(NodeType.OUTPUT_STRING);
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

    public static OutputString of(UUID uuid, String content) {
        return new OutputString(uuid, content);
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

    public Promise<Boolean> delete() {
        return null;
    }

}
