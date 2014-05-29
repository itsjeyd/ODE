package models.functions;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Function;


public class ExistsFunction implements Function<JsonNode, Boolean> {
    public Boolean apply(JsonNode json) {
        return json.get("data").size() > 0;
    }
}
