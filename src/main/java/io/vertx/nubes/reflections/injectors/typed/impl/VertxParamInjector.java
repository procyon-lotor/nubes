package io.vertx.nubes.reflections.injectors.typed.impl;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.nubes.reflections.injectors.typed.ParamInjector;

public class VertxParamInjector implements ParamInjector<Vertx> {

    @Override
    public Vertx resolve(RoutingContext context) {
        return context.vertx();
    }

}
