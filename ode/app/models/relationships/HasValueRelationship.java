package models.relationships;

import models.nodes.Feature;
import models.nodes.Rule;
import models.nodes.Value;


public class HasValueRelationship extends HasRelationship {
    public Feature startNode;
    public Value endNode;

    public HasValueRelationship(Feature startNode, Value endNode, Rule rule) {
        super(startNode, endNode, rule);
    }

}
