package models.nodes;

import constants.NodeType;
import java.util.UUID;
import play.libs.F.Promise;


public abstract class UUIDNode extends LabeledNodeWithProperties {

    public UUIDNode(NodeType label) {
        super(label);
    }

    public abstract Promise<UUID> getUUID();

}
