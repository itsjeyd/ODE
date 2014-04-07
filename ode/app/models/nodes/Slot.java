package models.nodes;

import java.util.UUID;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.SlotManager;
import models.relationships.HasSlotRelationship;


public class Slot extends LabeledNodeWithProperties {

    private Slot() {
        super(NodeType.SLOT);
    }

    private Slot(UUID uuid) {
        this();
        this.jsonProperties.put("uuid", uuid.toString());
    }

    private Slot(UUID uuid, int position) {
        this(uuid);
        this.jsonProperties.put("position", position);
    }

    public static Slot of(UUID uuid) {
        return new Slot(uuid);
    }

    public static Slot of(UUID uuid, int position) {
        return new Slot(uuid, position);
    }

    public Promise<Boolean> create() {
        return SlotManager.create(this);
    }

    public Promise<Boolean> connectTo(final CombinationGroup group) {
        Promise<Boolean> created = this.create();
        return created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        return new HasSlotRelationship(group, Slot.this)
                            .create();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> removeFrom(CombinationGroup group) {
        Promise<Boolean> disconnected = HasSlotRelationship
            .delete(group, this);
        return disconnected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean disconnected) {
                    if (disconnected) {
                        return Slot.this.delete();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> delete() {
        return SlotManager.delete(this);
    }

}
