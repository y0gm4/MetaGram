package org.carboncock.metagram.annotation;

import org.carboncock.metagram.annotation.types.PermissionType;
import org.carboncock.metagram.annotation.types.SendMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    /** <h3>The <b>class</b> that contains, as a field, the list of ids.<br>
     * <b>Field:</b> {@code List<Long>}
     * @throws org.carboncock.metagram.annotation.exception.ListFieldNotFoundException if the list cannot be found/accessed*/
    Class<?> listLocation();

    /** <h3>The type of permission that the <b>id list</b> will have</h3> */
    PermissionType type() default PermissionType.ABLE_TO_DO;

    /** <h3>The <b>method</b> to send the <b>error</b> to the user who has no permission</h3>
     * @throws org.carboncock.metagram.annotation.exception.IllegalSendingMethodException if you choose as a method <code>ANSWER_CALLBACK_QUERY</code>
     * or <code>MESSAGE_EDIT</code>
     * and the permission is for a <b>command</b>.*/
    SendMethod send();

    String onMissingPermission() default "";
}
