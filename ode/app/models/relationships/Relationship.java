package models.relationships;

import play.libs.F.Promise;

import models.Model;
import managers.relationships.RelationshipManager;
import models.nodes.LabeledNodeWithProperties;


public abstract class Relationship extends Model {
    public int ID;
    public LabeledNodeWithProperties startNode;
    public LabeledNodeWithProperties endNode;

    public Relationship() {
        this.ID = -1;
    }

}
