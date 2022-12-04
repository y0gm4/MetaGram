package org.carboncock.metagram.telegram.api;

import lombok.SneakyThrows;
import org.carboncock.metagram.annotation.Callback;
import org.carboncock.metagram.annotation.Permission;
import org.carboncock.metagram.annotation.PermissionHandler;
import org.carboncock.metagram.annotation.exception.IllegalMethodException;
import org.carboncock.metagram.annotation.exception.IllegalPermissionHandlerMethodException;
import org.carboncock.metagram.annotation.types.CallbackFilter;
import org.carboncock.metagram.annotation.types.PermissionType;
import org.carboncock.metagram.annotation.types.SendMethod;
import org.carboncock.metagram.listener.CallbackListener;
import org.carboncock.metagram.listener.Listener;
import org.carboncock.metagram.listener.Permissionable;
import org.carboncock.metagram.listener.UpdateListener;
import org.carboncock.metagram.telegram.data.CallbackData;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CallbackHandler implements UpdateListener {

    private final Map<Callback, Class<? extends CallbackListener>> queryMap = MetaGramApi.query;
    private final List<Listener> genericListener = MetaGramApi.genericListeners;

    protected CallbackHandler(){}

    @SneakyThrows
    @Override
    public void onUpdate(TelegramLongPollingBot bot, Update update) {
        if(!update.hasCallbackQuery()) return;
        String query = update.getCallbackQuery().getData();

        Optional<Method> callbackMethodOpt = getCallbackMethod(query);
        callbackMethodOpt.ifPresent(method -> {
            if(method.getParameterTypes().length != 1 && method.getParameterTypes()[0].equals(CallbackData.class))
                try {
                    throw new IllegalMethodException("The annotated method must have only 1 parameter of type CallbackData.class");
                } catch (IllegalMethodException e) {
                    e.printStackTrace();
                    return;
                }
            Class<?> mClass = method.getDeclaringClass();
            Permission permission = method.getAnnotation(Permission.class);
            Callback callback = method.getAnnotation(Callback.class);
            if(method.isAnnotationPresent(Permission.class) && !isPermitted(permission, mClass, bot, update)) return;
            CallbackData callbackData = new CallbackData(bot, update, getParams(query, callback));
            callbackData.setAnnotation(callback);
            callbackData.setPermission(Optional.ofNullable(permission));
            try {
                method.invoke(mClass.getDeclaredConstructor().newInstance(), callbackData);
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        Optional<Class<? extends CallbackListener>> optClass = getCallbackClass(query);
        if(!optClass.isPresent()) return;
        Class<? extends CallbackListener> clazz = optClass.get();
        if(clazz.isAnnotationPresent(Permission.class) && !isPermitted(clazz.getAnnotation(Permission.class), clazz, bot, update))
            return;
        Callback callback = clazz.getAnnotation(Callback.class);
        CallbackData callbackData = new CallbackData(bot, update, getParams(query, callback));
        callbackData.setAnnotation(callback);
        callbackData.setPermission(Optional.ofNullable(clazz.getAnnotation(Permission.class)));
        Method m = clazz.getMethod("onCallback", CallbackData.class);
        m.invoke(clazz.getDeclaredConstructor().newInstance(), callbackData);
    }


    private Optional<Class<? extends CallbackListener>> getCallbackClass(String query){
        for(Map.Entry<Callback, Class<? extends CallbackListener>> entry : queryMap.entrySet()){
            Callback c = entry.getKey();
            switch(c.filter()){
                case EQUALS:
                    return query.equalsIgnoreCase(c.value()) ? Optional.of(entry.getValue()) : Optional.empty();
                case START_WITH:
                    return query.startsWith(c.value()) ? Optional.of(entry.getValue()) : Optional.empty();
                case CONTAINS:
                    return query.contains(c.value()) ? Optional.of(entry.getValue()) : Optional.empty();
                case CUSTOM_PARAMETER:
                    return query.startsWith(c.value().substring(0, c.value().indexOf("=") + 1)) ? Optional.of(entry.getValue()) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<Method> getCallbackMethod(String query) {
        AtomicReference<Optional<Method>> method = new AtomicReference<>(Optional.empty());
        genericListener.forEach(listener -> {
            Class<? extends Listener> clazz = listener.getClass();
            method.set(Arrays.stream(clazz.getMethods())
                    .filter(m -> m.isAnnotationPresent(Callback.class))
                    .filter(m -> {
                        Callback callback = m.getAnnotation(Callback.class);
                        switch(callback.filter()){
                            case EQUALS:
                                return query.equalsIgnoreCase(callback.value());
                            case START_WITH:
                                return query.startsWith(callback.value());
                            case CONTAINS:
                                return query.contains(callback.value());
                            case CUSTOM_PARAMETER:
                                return query.startsWith(callback.value().substring(0, callback.value().indexOf("=") + 1));
                                // TODO check regex and add parameters
                        }
                        return false;
                    })
                    .findFirst());
        });
        return method.get();
    }

    private Map<String, Object> getParams(String data, Callback callback){
        Map<String, Object> params = new HashMap<>();
        String[] paramsNames = callback.value().substring(callback.value().indexOf("=") + 1)
                .replace("{", "")
                .replace("}", "")
                .split("&");
        String[] paramsValues = data.substring(data.indexOf("=") + 1).split("&");
        if(paramsNames.length != paramsValues.length || !callback.filter().equals(CallbackFilter.CUSTOM_PARAMETER))
            return params;
        for(int i = 0; i < paramsNames.length; i++)
            params.put(paramsNames[i], paramsValues[i]);
        return params;
    }

    @SneakyThrows
    private boolean isPermitted(Permission permission, Class<?> clazz,  TelegramLongPollingBot bot, Update update){
        User user = update.getCallbackQuery().getFrom();
        Class<?> listLocation = permission.listLocation();
        Optional<Method> optMethod = Arrays.stream(listLocation.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PermissionHandler.class))
                .findFirst();
        if(optMethod.isPresent()){
            try {
                Method method = optMethod.get();
                method.setAccessible(true);
                method.invoke(listLocation.getDeclaredConstructor().newInstance());
            }catch(Exception e){
                throw new IllegalPermissionHandlerMethodException(
                        String.format("Error, the method must be without arguments!\nClass: %s", listLocation.getName())
                );
            }
        }
        Class<?> locClass = permission.listLocation();
        Optional<Field> optField = Arrays.stream(locClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(PermissionHandler.class))
                .findFirst();
        if(!optField.isPresent()) return false;
        Field field = optField.get();
        field.setAccessible(true);
        List<Long> list = (List<Long>) field.get(locClass.getDeclaredConstructor().newInstance());

        if((list.contains(user.getId()) && permission.type().equals(PermissionType.UNABLE_TO_DO)) || (!list.contains(user.getId()) && permission.type().equals(PermissionType.ABLE_TO_DO))){
            SendMethod sendMethod = permission.send();
            String permMissing;
            if(Permissionable.class.isAssignableFrom(clazz))
                permMissing = ((Permissionable) clazz.getDeclaredConstructor().newInstance()).onPermissionMissing();
            else if(permission.onMissingPermission().equals("") && !permission.send().equals(SendMethod.NONE))
                throw new IllegalStateException("Missing \"onMissingPermission\" value for Permission.class annotation");
            else
                permMissing = permission.onMissingPermission();
            CallbackQuery callbackQuery = update.getCallbackQuery();
            switch(sendMethod){
                case SEND_MESSAGE:
                    SendMessage send = new SendMessage();
                    send.setChatId("" + update.getCallbackQuery().getMessage().getChatId());
                    send.enableHtml(true);
                    send.setText(permMissing);
                    bot.execute(send);
                    break;
                case EDIT_MESSAGE:
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId("" + update.getCallbackQuery().getMessage().getChatId());
                    edit.setMessageId(callbackQuery.getMessage().getMessageId());
                    edit.enableHtml(true);
                    edit.setText(permMissing);
                    bot.execute(edit);
                    break;
                case REPLY_MESSAGE:
                    SendMessage reply = new SendMessage();
                    reply.setChatId("" + update.getCallbackQuery().getMessage().getChatId());
                    reply.setReplyToMessageId(callbackQuery.getMessage().getMessageId());
                    reply.enableHtml(true);
                    reply.setText(permMissing);
                    bot.execute(reply);
                    break;
                case ANSWER_CALLBACK_QUERY:
                    AnswerCallbackQuery acq = new AnswerCallbackQuery();
                    acq.setCallbackQueryId(callbackQuery.getId());
                    acq.setShowAlert(true);
                    acq.setText(permMissing);
                    bot.executeAsync(acq);
                    break;
                default:
                    break;
            }
            return false;
        }
        return true;
    }
}
