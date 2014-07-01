import play.GlobalSettings;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.SimpleResult;
import play.mvc.Results;
import views.html.error;
import views.html.missing;


public class Global extends GlobalSettings {

    @Override
    public Promise<SimpleResult> onError(
        Http.RequestHeader request, Throwable t) {
        return Promise.<SimpleResult>pure(
            Results.internalServerError(error.render()));
    }

    @Override
    public Promise<SimpleResult> onHandlerNotFound(
        Http.RequestHeader request) {
        return Promise.<SimpleResult>pure(
            Results.notFound(missing.render()));
    }

    @Override
    public Promise<SimpleResult> onBadRequest(
        Http.RequestHeader request, String error) {
        return Promise.<SimpleResult>pure(
            Results.badRequest(missing.render()));
    }

}
