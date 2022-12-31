package org.carboncock.metagram.filters;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CustomFilters.class)
public @interface CustomFilter {
    Class<?> cfLocation() default Class.class;
    String value();
}
