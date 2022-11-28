package org.carboncock.metagram.telegram.data;

import lombok.Getter;
import lombok.Setter;
import org.carboncock.metagram.annotation.Permission;
import org.carboncock.metagram.annotation.PermissionHandler;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public abstract class Data<A extends Annotation> {

    protected TelegramLongPollingBot botInstance;

    protected Update update;

    protected A annotation;

    protected Optional<Permission> permission;

    protected List<Long> userPermitted;

    protected abstract void onProcessAnnotation(A annotation);

    public void setAnnotation(A annotation){
        this.annotation = annotation;
        onProcessAnnotation(annotation);
    }

    public void setPermission(Optional<Permission> permission){
        this.permission = permission;

        if(!permission.isPresent()) return;
        Class<?> locClass = permission.get().listLocation();
        Optional<Field> optField = Arrays.stream(locClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(PermissionHandler.class))
                .findFirst();

        optField.ifPresent(field -> {
            field.setAccessible(true);
            try {
                userPermitted = (List<Long>) field.get(locClass.getDeclaredConstructor().newInstance());
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
    }

}
