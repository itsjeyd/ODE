package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.PartManager;
import models.relationships.HasPartRelationship;


public class Part extends LabeledNodeWithProperties {
    public String content;

    private Part(String content) {
        super(NodeType.PART);
        this.content = content;
        this.jsonProperties.put("content", content);
    }

    public static Part of(String content) {
        return new Part(content);
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

    public static Promise<List<Part>> all() {
        Promise<List<JsonNode>> json = PartManager.all();
        Promise<List<Part>> parts = json.map(new AllFunction());
        return parts;
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

    public Promise<Boolean> delete() {
        return Promise.pure(false);
    }

    private static class AllFunction
        implements Function<List<JsonNode>, List<Part>> {
        public List<Part> apply(List<JsonNode> dataNodes) {
            List<Part> parts = new ArrayList<Part>();
            for (JsonNode dataNode: dataNodes) {
                String content = dataNode.get("content").asText();
                parts.add(new Part(content));
            }
            return parts;
        }
    }

}
