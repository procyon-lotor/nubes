package com.github.aesteve.vertx.nubes.handlers.impl;

import com.github.aesteve.vertx.nubes.context.PaginationContext;
import com.github.aesteve.vertx.nubes.handlers.Processor;
import com.google.common.net.HttpHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class PaginationProcessor extends NoopAfterAllProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(PaginationProcessor.class);

    @Override
    public void preHandle(RoutingContext context) {
        context.data().put(PaginationContext.DATA_ATTR, PaginationContext.fromContext(context));
        context.next();
    }

    @Override
    public void postHandle(RoutingContext context) {
        PaginationContext pageContext = (PaginationContext) context.data().get(PaginationContext.DATA_ATTR);
        String linkHeader = pageContext.buildLinkHeader(context.request());
        if (linkHeader != null) {
            context.response().headers().add(HttpHeaders.LINK, linkHeader);
        } else {
            LOG.warn("You did not set the total count on PaginationContext, response won't be paginated");
        }
        context.next();
    }

}
