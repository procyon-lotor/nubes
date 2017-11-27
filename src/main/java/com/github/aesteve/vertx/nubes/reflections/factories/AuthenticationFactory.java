package com.github.aesteve.vertx.nubes.reflections.factories;

import com.github.aesteve.vertx.nubes.Config;
import com.github.aesteve.vertx.nubes.annotations.auth.Auth;
import com.github.aesteve.vertx.nubes.auth.AuthMethod;
import static com.github.aesteve.vertx.nubes.auth.AuthMethod.API_TOKEN;
import static com.github.aesteve.vertx.nubes.auth.AuthMethod.BASIC;
import static com.github.aesteve.vertx.nubes.auth.AuthMethod.JWT;
import static com.github.aesteve.vertx.nubes.auth.AuthMethod.REDIRECT;
import com.github.aesteve.vertx.nubes.handlers.impl.CheckTokenHandler;
import io.vertx.core.Handler;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class AuthenticationFactory {

    private final Config config;
    private Map<AuthMethod, Function<AuthProvider, Handler<RoutingContext>>> authHandlers;

    public AuthenticationFactory(Config config) {
        this.config = config;
        authHandlers = new EnumMap<>(AuthMethod.class);
        authHandlers.put(BASIC, BasicAuthHandler::create);
        authHandlers.put(JWT, auth -> JWTAuthHandler.create((JWTAuth) config.getAuthProvider()));
        authHandlers.put(API_TOKEN, CheckTokenHandler::new);
    }

    public Handler<RoutingContext> create(Auth auth) {
        final AuthProvider authProvider = config.getAuthProvider();
        if (authProvider == null) {
            return null;
        }
        final AuthMethod authMethod = auth.method();
        Function<AuthProvider, Handler<RoutingContext>> authHandlerCreator = authHandlers.get(authMethod);
        if (authHandlerCreator == null && authMethod == REDIRECT) {
            final String redirect = auth.redirectURL();
            if ("".equals(redirect)) {
                throw new IllegalArgumentException("You must specify a redirectURL if you're using Redirect Auth");
            }
            return RedirectAuthHandler.create(authProvider, redirect);
        } else if (authHandlerCreator == null) {
            throw new UnsupportedOperationException();
        }
        return authHandlerCreator.apply(authProvider);
    }
}
