package models.nodes;

import constants.NodeType;
import play.libs.F.Promise;


public abstract class OntologyNode extends LabeledNodeWithProperties {
    public String name;

    protected OntologyNode(NodeType label) {
        super(label);
    }

}
