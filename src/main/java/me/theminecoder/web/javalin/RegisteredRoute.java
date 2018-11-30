package me.theminecoder.web.javalin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class RegisteredRoute {

    private String path;
    private Class<? extends Annotation> routeMethodType;
    private Method method;

    RegisteredRoute(String path, Class<? extends Annotation> routeMethodType, Method method) {
        this.path = path;
        this.routeMethodType = routeMethodType;
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public Class<? extends Annotation> getRouteMethodType() {
        return routeMethodType;
    }

    public Method getMethod() {
        return method;
    }
}
