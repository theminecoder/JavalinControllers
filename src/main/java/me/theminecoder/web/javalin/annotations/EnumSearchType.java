package me.theminecoder.web.javalin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnumSearchType {

    public static enum Type {
        CASE_SENSITIVE,
        CASE_INSENSITIVE
    }

    Type value();

}
