package org.carboncock.metagram.filters;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Filters {
    FilterType[] value();
    String[] fromWho() default FilterType.By.EVERYONE;
    String[] fromWhere() default FilterType.Chat.EVERYWHERE;
    String regex() default "";
}
