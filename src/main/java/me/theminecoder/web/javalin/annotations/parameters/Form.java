package me.theminecoder.web.javalin.annotations.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Form {

    public static final String NO_DEFAULT = "";

    String value();

    String defaultValue() default NO_DEFAULT;

}
