package org.carboncock.metagram.annotations;

import org.carboncock.metagram.annotations.types.CallbackFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Callback {
     String value();
     CallbackFilter filter() default CallbackFilter.EQUALS;
}
