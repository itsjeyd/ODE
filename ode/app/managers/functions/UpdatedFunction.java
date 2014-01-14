package managers.functions;

import play.libs.WS;
import play.libs.F.Function;
import play.mvc.Http.Status;


public class UpdatedFunction implements Function<WS.Response, Boolean> {

    public Boolean apply(WS.Response response) {
        return response.getStatus() == Status.NO_CONTENT;
    }

}
