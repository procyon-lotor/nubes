package com.github.aesteve.vertx.nubes.annotations.auth;

import com.github.aesteve.vertx.nubes.auth.AuthMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Auth {

    AuthMethod method();

    String authority();

    String redirectURL() default "";
}
