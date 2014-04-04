package models.nodes;

import java.util.UUID;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.OutputStringManager;
import models.relationships.HasStringRelationship;


public class OutputString extends LabeledNodeWithProperties {

    private OutputString() {
        super(NodeType.OUTPUT_STRING);
    }

    private OutputString(String content) {
        this();
        this.jsonProperties.put("content", content);
    }

    public static OutputString of(String content) {
        return new OutputString(content);
    }

    public Promise<Boolean> create() {
        return OutputStringManager.create(this);
    }

    public Promise<Boolean> connectTo(final CombinationGroup group) {
        Promise<Boolean> exists = this.exists();
        return exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return new HasStringRelationship(
                            group, OutputString.this).create();
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
