package managers.functions;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;


public class PropertyFunction implements Function<JsonNode, String> {

    private String propName;

    public PropertyFunction(String propName) {
        this.propName = propName;
    }

    public String apply(JsonNode json) {
        return json.findValue(propName).asText();
    }

}
