package models.nodes;

import constants.NodeType;
import java.util.List;
import managers.nodes.FeatureManager;


public class Feature extends OntologyNode {

    public static final FeatureManager nodes = new FeatureManager();

    protected String description;
    protected String type;
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
        this.type = type;
    }

    public Feature(String name, String description, String type) {
        this(name);
        this.description = description;
        this.type = type;
    }

    public Feature(
        String name, String description, String type, String uuid) {
        this(name);
        this.description = description;
        this.type = type;
        this.jsonProperties.put("uuid", uuid);
    }

    public String getType() {
        return this.type.toString();
    }

    public String getDescription() {
        return this.description;
    }

}
