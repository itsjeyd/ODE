package models.nodes;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.FeatureManager;
import managers.nodes.ValueManager;
import models.relationships.AllowsRelationship;


public abstract class OntologyNode extends LabeledNodeWithProperties {
    public String name;

    protected OntologyNode(NodeType label) {
        super(label);
    }

    public Boolean isFeature() {
        return this.label.equals(NodeType.FEATURE);
    }

    public Boolean isValue() {
        return this.label.equals(NodeType.VALUE);
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction(this));
    }

    public Promise<Boolean> connectTo(Feature feature) {
        return new AllowsRelationship(feature, this).create();
    }

    public abstract Promise<Boolean> delete();


    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private OntologyNode node;
        public CreateFunction(OntologyNode node) {
            this.node = node;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            if (this.node.isFeature()) {
                return FeatureManager.create((Feature) this.node);
            } else {
                return ValueManager.create((Value) this.node);
            }
        }
    }

}
