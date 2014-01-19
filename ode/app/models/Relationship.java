package models;

import play.libs.F.Promise;

import managers.RelationshipManager;


public abstract class Relationship extends Model {
    public int ID;
    public LabeledNodeWithProperties startNode;
    public LabeledNodeWithProperties endNode;

    public Promise<Boolean> delete() {
        return RelationshipManager.delete(this);
    };

}
