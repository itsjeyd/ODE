package managers.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;


public class NodeListFunction implements
                                  Function<WS.Response, List<JsonNode>> {

    public List<JsonNode> apply(WS.Response response) {
        JsonNode json = response.asJson();
        return json.findValues("data");
    }

}
