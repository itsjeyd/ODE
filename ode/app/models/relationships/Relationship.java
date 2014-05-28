package models.relationships;

import models.Model;
import models.nodes.LabeledNodeWithProperties;


public abstract class Relationship extends Model {
    public int ID;
    public LabeledNodeWithProperties startNode;
    public LabeledNodeWithProperties endNode;

    public Relationship() {
        this.ID = -1;
    }

}
