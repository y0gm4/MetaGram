package org.carboncock.metagram.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** <h1>Marker annotation</h1>
 * The annotated method <i>will be executed before the permission check</i>.
 * This will be used to <b>update ids</b> in case, for example, they are taken from a database.*/

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionHandler {
}
