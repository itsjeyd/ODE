package models.nodes;

import play.libs.F.Promise;

import constants.NodeType;


public abstract class OntologyNode extends LabeledNodeWithProperties {
    public String name;

    protected OntologyNode(NodeType label) {
        super(label);
    }

    public Promise<Boolean> delete() {
        return null;
    }

}
