package managers.functions;

import play.libs.WS;
import play.mvc.Http.Status;


public class UpdatedFunction extends BooleanFunction {

    public Boolean apply(WS.Response response) {
        return response.getStatus() == Status.NO_CONTENT;
    }

}
