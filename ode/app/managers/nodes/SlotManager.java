package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import models.nodes.Part;
import models.nodes.Slot;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;


public class SlotManager extends LabeledNodeWithPropertiesManager {

    public SlotManager() {
        this.label = "Slot";
    }

    protected Promise<Boolean> connect(
        final JsonNode slot, final JsonNode part, final String location) {
        Promise<Boolean> exists = Part.nodes.create(part, location);
        Promise<Boolean> connected = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        final Slot s = new Slot(slot.get("uuid").asText());
                        final Part p = new Part(
                            part.get("uuid").asText(),
                            part.get("content").asText());
                        Promise<Boolean> connected = Has.relationships
                            .exists(s, p);
                        return connected.flatMap(
                            new Function<Boolean, Promise<Boolean>>() {
                                public Promise<Boolean> apply(
                                    Boolean connected) {
                                    if (connected) {
                                        return Promise.pure(false);
                                    }
                                    return Has.relationships
                                        .create(s, p, location);
                                }
                            });
                    }
                    return Promise.pure(false);
                }
            });
        return connected;
    }

}
