package org.carboncock.metagram.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String value();
    char prefix() default '/';
    int args() default 0;
    String[] aliases() default "";
    boolean checkedArgs() default true;
}
