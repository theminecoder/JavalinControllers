package me.theminecoder.web.javalin;

import io.javalin.Context;

import java.lang.annotation.Annotation;

public interface ParameterMapper<T extends Annotation> {

    public Object map(Context ctx, T annotation, Class<?> argType);

}
