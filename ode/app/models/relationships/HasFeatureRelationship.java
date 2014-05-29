package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import managers.relationships.HasFeatureRelationshipManager;
import models.nodes.AVM;
import models.nodes.Feature;
import play.libs.F.Function;
import play.libs.F.Promise;


public class HasFeatureRelationship extends HasRelationship {
    public AVM startNode;
    public Feature endNode;

    public HasFeatureRelationship(AVM startNode, Feature endNode) {
        super(startNode, endNode, startNode.rule);
    }

    public static Promise<List<Feature>> getEndNodes(final AVM startNode) {
        Promise<List<JsonNode>> endNodes = HasFeatureRelationshipManager
            .getEndNodes(startNode);
        return endNodes.map(
            new Function<List<JsonNode>, List<Feature>>() {
                public List<Feature> apply(List<JsonNode> featureNodes) {
                    List<Feature> features = new ArrayList<Feature>();
                    for (JsonNode featureNode: featureNodes) {
                        String name = featureNode.findValue("name").asText();
                        String type = featureNode.findValue("type").asText();
                        Feature feature = new Feature(name, type);
                        features.add(feature);
                    }
                    return features;
                }
            });
    }

}
