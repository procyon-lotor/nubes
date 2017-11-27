package com.github.aesteve.vertx.nubes.reflections.visitors;

import com.github.aesteve.vertx.nubes.Config;
import com.github.aesteve.vertx.nubes.annotations.Controller;
import com.github.aesteve.vertx.nubes.annotations.filters.AfterFilter;
import com.github.aesteve.vertx.nubes.annotations.filters.BeforeFilter;
import com.github.aesteve.vertx.nubes.handlers.AnnotationProcessor;
import com.github.aesteve.vertx.nubes.handlers.Processor;
import com.github.aesteve.vertx.nubes.reflections.Filter;
import com.github.aesteve.vertx.nubes.reflections.RouteRegistry;
import com.github.aesteve.vertx.nubes.reflections.factories.AuthenticationFactory;
import com.github.aesteve.vertx.nubes.routing.MVCRoute;
import io.vertx.core.VertxException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

public class ControllerVisitor<T> extends BaseVisitor<T> {

    private final Method[] methods;
    final String basePath;
    final AuthenticationFactory authFactory;
    final RouteRegistry routeRegistry;
    final Map<Class<? extends Annotation>, BiConsumer<RoutingContext, ?>> returnHandlers;
    // 记录注解处理器
    final Set<Processor> processors;
    // 前置拦截器
    private final Set<Filter> beforeFilters;
    // 后置拦截器
    private final Set<Filter> afterFilters;

    public ControllerVisitor(Class<T> controllerClass, Config config, Router router, AuthenticationFactory authFactory,
                             RouteRegistry routeRegistry, Map<Class<? extends Annotation>, BiConsumer<RoutingContext, ?>> returnHandlers) {
        super(controllerClass, config, router);
        this.routeRegistry = routeRegistry;
        this.returnHandlers = returnHandlers;
        this.methods = controllerClass.getDeclaredMethods();
        Controller base = clazz.getAnnotation(Controller.class);
        this.basePath = base.value();
        this.authFactory = authFactory;
        this.processors = new LinkedHashSet<>();
        this.beforeFilters = new TreeSet<>();
        this.afterFilters = new TreeSet<>();
    }

    public List<MVCRoute> visit() throws IllegalAccessException, InstantiationException {
        this.instance = clazz.newInstance();
        List<MVCRoute> routes = new ArrayList<>();
        try {
            // 依赖注入
            this.injectServices();
        } catch (IllegalAccessException iae) {
            throw new VertxException(iae);
        }
        // 抽取过滤器
        this.extractFilters();
        // 抽取参数对应的处理器
        for (Method method : methods) {
            MethodVisitor<T> visitor = new MethodVisitor<>(this, method);
            routes.addAll(visitor.visit());
        }
        for (MVCRoute route : routes) {
            route.addProcessorsFirst(processors);
            route.addBeforeFilters(beforeFilters);
            route.addAfterFilters(afterFilters);
        }
        return routes;
    }

    private void extractFilters() {
        if (this.hasSuperClass()) {
            ControllerVisitor<?> superClass =
                    new ControllerVisitor<>(clazz.getSuperclass(), config, router, authFactory, routeRegistry, returnHandlers);
            superClass.extractFilters();
            beforeFilters.addAll(superClass.beforeFilters);
            afterFilters.addAll(superClass.afterFilters);
            processors.addAll(superClass.processors);
        }
        for (Method method : methods) {
            BeforeFilter beforeFilter = method.getAnnotation(BeforeFilter.class);
            AfterFilter afterFilter = method.getAnnotation(AfterFilter.class);
            if (beforeFilter != null) {
                beforeFilters.add(new Filter(method, beforeFilter));
            } else if (afterFilter != null) {
                afterFilters.add(new Filter(method, afterFilter));
            }
        }
        for (Annotation annotation : clazz.getDeclaredAnnotations()) {
            // 获取注解对应的处理器
            AnnotationProcessor<?> controllerProcessor = config.getAnnotationProcessor(annotation);
            if (controllerProcessor != null) {
                // 注册注解处理器
                processors.add(controllerProcessor);
            }
        }
    }

    private boolean hasSuperClass() {
        return clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class);
    }

}
