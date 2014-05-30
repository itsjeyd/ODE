package models.nodes;

import java.nio.charset.Charset;
import java.util.UUID;
import managers.nodes.AVMManager;
import play.libs.F.Function;
import play.libs.F.Promise;


public class Substructure extends AVM {

    public static final AVMManager nodes = new AVMManager();

    public AVM parent;
    public Feature embeddingFeature;

    protected Substructure(Rule rule, UUID uuid) {
        super(rule);
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public Substructure(Rule rule, AVM parent, Feature embeddingFeature) {
        super(rule);
        this.parent = parent;
        this.embeddingFeature = embeddingFeature;
    }

    public Substructure(String uuid) {
        this.jsonProperties.put("uuid", uuid);
    }

}
