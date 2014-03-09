package models.nodes;

import constants.NodeType;


public abstract class LabeledNode extends Node {
    public NodeType label;

    public String getLabel() {
        return this.label.toString();
    }
}
