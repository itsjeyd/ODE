package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.nodes.CombinationGroup;
import models.nodes.OutputString;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;


public class CombinationGroupManager extends
                                         LabeledNodeWithPropertiesManager {

    public CombinationGroupManager() {
        this.label = "CombinationGroup";
    }

    protected Promise<Boolean> connect(
        final JsonNode group, final JsonNode string, final String location) {
        Promise<Boolean> exists = OutputString.nodes.create(string, location);
        Promise<Boolean> connected = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        final CombinationGroup g =
                            new CombinationGroup(group.get("uuid").asText());
                        final OutputString s = new OutputString(
                            string.get("uuid").asText(),
                            string.get("content").asText());
                        Promise<Boolean> connected = Has.relationships
                            .exists(g, s);
                        return connected.flatMap(
                            new Function<Boolean, Promise<Boolean>>() {
                                public Promise<Boolean> apply(
                                    Boolean connected) {
                                    if (connected) {
                                        return Promise.pure(false);
                                    }
                                    return Has.relationships
                                        .create(g, s, location);
                                }
                            });

                    }
                    return Promise.pure(false);
                }
            });
        return connected;
    }

    protected Promise<Boolean> disconnect(
        JsonNode group, JsonNode string, String location) {
        CombinationGroup g = new CombinationGroup(group.get("uuid").asText());
        OutputString s = new OutputString(string.get("uuid").asText());
        return Has.relationships.delete(g, s, location);
    }

    public static Promise<Boolean> update(CombinationGroup group,
                                          int position) {
        ObjectNode newProps = group.jsonProperties.deepCopy();
        newProps.put("position", position);
        return LabeledNodeWithPropertiesManager
            .updateProperties(group, newProps);
    }

}
