package models;

import play.libs.F.Function;
import play.libs.F.None;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Some;
import play.libs.F.Tuple;

import constants.NodeType;


public abstract class OntologyNode extends LabeledNodeWithProperties {
    public String name;

    public Boolean isFeature() {
        return this.label.equals(NodeType.FEATURE);
    }

    public Boolean isValue() {
        return this.label.equals(NodeType.VALUE);
    }

    public Promise<Tuple<Option<OntologyNode>, Boolean>> getOrCreate() {
        return this.exists().flatMap(new GetOrCreateFunction(this));
    }


    private class GetOrCreateFunction
        implements Function<Boolean,
                            Promise<Tuple<Option<OntologyNode>, Boolean>>> {
        private OntologyNode node;
        public GetOrCreateFunction(OntologyNode node) {
            this.node = node;
        }
        public Promise<Tuple<Option<OntologyNode>, Boolean>> apply(
            Boolean exists) {
            if (exists) {
                return Promise.pure(
                    new Tuple<Option<OntologyNode>, Boolean>(
                        new Some<OntologyNode>(this.node), false));
            }
            Promise<Boolean> created = null;
            if (this.node.isFeature()) {
                created = Feature.Manager.create((Feature) this.node);
            } else {
                created = Value.Manager.create((Value) this.node);
            }
            return created.map(new CreatedFunction(this.node));
        }
    }

    private class CreatedFunction
        implements Function<Boolean, Tuple<Option<OntologyNode>, Boolean>> {
        private OntologyNode node;
        public CreatedFunction(OntologyNode node) {
            this.node = node;
        }
        public Tuple<Option<OntologyNode>, Boolean> apply(
            Boolean created) {
            if (created) {
                return new Tuple<Option<OntologyNode>, Boolean>(
                    new Some<OntologyNode>(this.node), true);
            }
            return new Tuple<Option<OntologyNode>, Boolean>(
                new None<OntologyNode>(), false);
        }
    }

}
