package mock.controllers.errors;

import io.vertx.ext.web.RoutingContext;
import io.vertx.nubes.annotations.Controller;
import io.vertx.nubes.annotations.mixins.ContentType;
import io.vertx.nubes.annotations.routing.Path;
import io.vertx.nubes.exceptions.BadRequestException;

@Controller("/errors/json")
@ContentType("application/json")
public class ErrorsJsonController {

    @Path("/fail/:statusCode")
    public void throwErrorUsingFail(RoutingContext context, Integer status) {
        switch (status) {
            case 400:
                context.fail(new BadRequestException("Deliberately failed with bad request"));
                break;
            case 500:
                context.fail(new RuntimeException("Deliberately failed with runtime exception"));
                break;
        }
    }

    @Path("/throw/:statusCode")
    public void throwError(RoutingContext context, Integer status) throws Exception {
        switch (status) {
            case 400:
                throw new BadRequestException("Deliberately thrown bad request");
            case 500:
                throw new RuntimeException("Deliberately thrown runtime exception");
        }
    }
}
