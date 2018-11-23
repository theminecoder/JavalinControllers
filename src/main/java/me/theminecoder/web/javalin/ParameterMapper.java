package me.theminecoder.web.javalin;

import io.javalin.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public interface ParameterMapper<T extends Annotation> {

    public Object map(Context ctx, T annotation, Class<?> argType);

    public default Object map(Context ctx, T annotation, Class<?> argType, Parameter orginalParameter) {
        return map(ctx, annotation, argType);
    }

}
