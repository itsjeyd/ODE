package models.nodes;

import java.util.UUID;

import play.libs.F.Promise;

import constants.NodeType;


public abstract class UUIDNode extends LabeledNodeWithProperties {

    public UUIDNode(NodeType label) {
        super(label);
    }

    public abstract Promise<UUID> getUUID();

}
