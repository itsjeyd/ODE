package models.nodes;

import constants.NodeType;


public abstract class OntologyNode extends LabeledNodeWithProperties {
    public String name;

    protected OntologyNode(NodeType label) {
        super(label);
    }

}
