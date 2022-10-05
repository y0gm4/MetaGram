package org.carboncock.metagram.telegram.api;

import lombok.SneakyThrows;
import org.carboncock.metagram.annotation.Callback;
import org.carboncock.metagram.annotation.Permission;
import org.carboncock.metagram.annotation.PermissionHandler;
import org.carboncock.metagram.annotation.exception.IllegalPermissionHandlerMethodException;
import org.carboncock.metagram.annotation.types.PermissionType;
import org.carboncock.metagram.annotation.types.SendMethod;
import org.carboncock.metagram.listener.CallbackListener;
import org.carboncock.metagram.listener.Permissionable;
import org.carboncock.metagram.listener.UpdateListener;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CallbackHandler implements UpdateListener {

    private final Map<Callback, Class<? extends CallbackListener>> queryMap = MetaGramApi.query;

    protected CallbackHandler(){}

    @SneakyThrows
    @Override
    public void onUpdate(TelegramLongPollingBot bot, Update update) {
        if(!update.hasCallbackQuery()) return;
        String query = update.getCallbackQuery().getData();
        Optional<Class<? extends CallbackListener>> optClass = getCallbackClass(query);

        if(!optClass.isPresent()) return;
        Class<? extends CallbackListener> clazz = optClass.get();
        User user = update.getCallbackQuery().getFrom();
        if(clazz.isAnnotationPresent(Permission.class) && Permissionable.class.isAssignableFrom(clazz)){
            Permission permission = clazz.getAnnotation(Permission.class);
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
            if(!optField.isPresent()) return;
            Field field = optField.get();
            field.setAccessible(true);
            List<Long> list = (List<Long>) field.get(locClass.getDeclaredConstructor().newInstance());

            if((list.contains(user.getId()) && permission.type().equals(PermissionType.UNABLE_TO_DO)) || (!list.contains(user.getId()) && permission.type().equals(PermissionType.ABLE_TO_DO))){
                SendMethod sendMethod = permission.send();
                Permissionable permClass = (Permissionable) clazz.getDeclaredConstructor().newInstance();
                CallbackQuery callbackQuery = update.getCallbackQuery();
                switch(sendMethod){
                    case SEND_MESSAGE:
                        SendMessage send = new SendMessage();
                        send.setChatId("" + user.getId());
                        send.enableHtml(true);
                        send.setText(permClass.onPermissionMissing());
                        bot.execute(send);
                        break;
                    case EDIT_MESSAGE:
                        EditMessageText edit = new EditMessageText();
                        edit.setChatId("" + user.getId());
                        edit.setMessageId(callbackQuery.getMessage().getMessageId());
                        edit.enableHtml(true);
                        edit.setText(permClass.onPermissionMissing());
                        bot.execute(edit);
                        break;
                    case REPLY_MESSAGE:
                        SendMessage reply = new SendMessage();
                        reply.setChatId("" + user.getId());
                        reply.setReplyToMessageId(callbackQuery.getMessage().getMessageId());
                        reply.enableHtml(true);
                        reply.setText(permClass.onPermissionMissing());
                        bot.execute(reply);
                        break;
                    case ANSWER_CALLBACK_QUERY:
                        AnswerCallbackQuery acq = new AnswerCallbackQuery();
                        acq.setCallbackQueryId(callbackQuery.getId());
                        acq.setShowAlert(true);
                        acq.setText(permClass.onPermissionMissing());
                        bot.executeAsync(acq);
                        break;
                    default:
                        break;
                }
                return;
            }
        }
        Method m = clazz.getMethod("onCallback", TelegramLongPollingBot.class, Update.class);
        m.invoke(clazz.getDeclaredConstructor().newInstance(), bot, update);
    }


    private Optional<Class<? extends CallbackListener>> getCallbackClass(String query){
        for(Map.Entry<Callback, Class<? extends CallbackListener>> entry : queryMap.entrySet()){
            Callback c = entry.getKey();
            switch(c.filter()){
                case EQUALS:
                    return query.equalsIgnoreCase(c.query()) ? Optional.of(entry.getValue()) : Optional.empty();
                case START_WITH:
                    return query.startsWith(c.query()) ? Optional.of(entry.getValue()) : Optional.empty();
                default:
                    return query.contains(c.query()) ? Optional.of(entry.getValue()) : Optional.empty();
            }
        }
        return Optional.empty();
    }
}
