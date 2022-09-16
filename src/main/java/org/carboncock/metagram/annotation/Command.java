package org.carboncock.metagram.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String name();
    int args() default 0;
    String[] aliases() default "";
    boolean checkedArgs() default true;
}
