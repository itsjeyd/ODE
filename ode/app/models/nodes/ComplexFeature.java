package models.nodes;

import constants.FeatureType;


public class ComplexFeature extends Feature {

    public ComplexFeature(String name) {
        super(name);
        this.type = FeatureType.COMPLEX;
    }

    public ComplexFeature(String name, String description) {
        super(name, description);
        this.type = FeatureType.COMPLEX;
    }

}
