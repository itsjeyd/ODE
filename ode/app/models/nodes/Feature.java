package models.nodes;

import constants.FeatureType;
import constants.NodeType;
import java.util.List;
import managers.nodes.FeatureManager;


public class Feature extends OntologyNode {

    public static final FeatureManager nodes = new FeatureManager();

    protected FeatureType type;
    protected String description;
    public List<String> targets;

    private Feature() {
        super(NodeType.FEATURE);
    }

    public Feature(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Feature(String name, String type) {
        this(name);
        this.setType(type);
    }

    public Feature(String name, String description, String type) {
        this(name);
        this.description = description;
        this.setType(type);
    }

    protected Feature setType(String type) {
        if (type.equals(FeatureType.COMPLEX.toString())) {
            this.type = FeatureType.COMPLEX;
        } else {
            this.type = FeatureType.ATOMIC;
        }
        return this;
    }

    public String getType() {
        return this.type.toString();
    }

    public String getDescription() {
        return this.description;
    }

}
