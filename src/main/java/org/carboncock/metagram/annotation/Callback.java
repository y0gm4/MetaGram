package org.carboncock.metagram.annotation;

import org.carboncock.metagram.annotation.types.CallbackFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Callback {
     String query();
     CallbackFilter filter() default CallbackFilter.EQUALS;
}
