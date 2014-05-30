package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import managers.nodes.LHSManager;
import play.libs.F.Function;
import play.libs.F.Promise;


public class LHS extends AVM {

    public static final LHSManager nodes = new LHSManager();

    public Rule parent;

    public LHS(String uuid) {
        this.jsonProperties.put("uuid", uuid);
    }

    public LHS(Rule rule) {
        super(rule);
        this.parent = rule;
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.map(new UUIDFunction());
    }

    public Promise<List<Feature>> getAllFeatures() {
        Promise<JsonNode> json = this.toJSON();
        return json.map(
            new Function<JsonNode, List<Feature>>() {
                public List<Feature> apply(JsonNode json) {
                    List<Feature> features = new ArrayList<Feature>();
                    List<JsonNode> featureNodes = json
                        .findValues("attribute");
                    for (JsonNode featureNode : featureNodes) {
                        String name = featureNode.findValue("name").asText();
                        features.add(new Feature(name));
                    }
                    return features;
                }
            });
    }

    protected static class UUIDFunction implements Function<UUID, UUID> {
        public UUID apply(UUID parentUUID) {
            byte[] bytes = parentUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            return UUID.nameUUIDFromBytes(bytes);
        }
    }

}
