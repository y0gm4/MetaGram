package org.carboncock.metagram.telegram.api;

import lombok.SneakyThrows;
import org.carboncock.metagram.annotation.Command;
import org.carboncock.metagram.annotation.Permission;
import org.carboncock.metagram.annotation.PermissionHandler;
import org.carboncock.metagram.annotation.exception.IllegalPermissionHandlerMethodException;
import org.carboncock.metagram.annotation.types.PermissionType;
import org.carboncock.metagram.annotation.types.SendMethod;
import org.carboncock.metagram.listener.CommandListener;
import org.carboncock.metagram.listener.Permissionable;
import org.carboncock.metagram.listener.UpdateListener;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class CommandHandler implements UpdateListener {

    private final Map<Command, Class<? extends CommandListener>> commands = MetaGramApi.commands;

    protected CommandHandler(){}

    @SneakyThrows
    @Override
    public void onUpdate(TelegramLongPollingBot bot, Update update) {
        if(!update.hasMessage()) return;
        if(!update.getMessage().hasText()) return;
        if(!update.getMessage().getText().startsWith("/")) return;

        String command = update.getMessage().getText().substring(1);
        String headCommand = command;
        User user = update.getMessage().getFrom();
        if(command.split(" ").length > 1) headCommand = command.substring(0, command.indexOf(" "));

        if(headCommand.equalsIgnoreCase("help")){
            Optional<Class<? extends CommandListener>> optClass = getCommandClass(command.replace(headCommand, "").trim());
            if(!optClass.isPresent()) return;
            Class<? extends CommandListener> clazz = optClass.get();
            Method m = clazz.getMethod("onHelpCommand", TelegramLongPollingBot.class, Update.class);
            m.invoke(clazz.getDeclaredConstructor().newInstance(), bot, update);
            return;
        }

        Optional<Class<? extends CommandListener>> optClass = getCommandClass(headCommand);
        if(!optClass.isPresent()) return;

        Class<? extends CommandListener> clazz = optClass.get();
        Command c = clazz.getAnnotation(Command.class);

        if(c.checkedArgs() && c.args() != command.split(" ").length - 1) return;

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
                SendMethod send = permission.send();
                if(send.equals(SendMethod.NONE)) return;
                Permissionable permClass = (Permissionable) clazz.getDeclaredConstructor().newInstance();
                SendMessage mex = new SendMessage();
                mex.setText(permClass.onPermissionMissing());
                mex.enableHtml(true);
                mex.setChatId("" + user.getId());
                if(send.equals(SendMethod.REPLY_MESSAGE))
                    mex.setReplyToMessageId(update.getMessage().getMessageId());
                bot.execute(mex);
                return;
            }
        }
        Method m = clazz.getMethod("onCommand", TelegramLongPollingBot.class, Update.class);
        m.invoke(clazz.getDeclaredConstructor().newInstance(), bot, update);
    }

    private Optional<Class<? extends CommandListener>> getCommandClass(String command) {
        for(Map.Entry<Command, Class<? extends CommandListener>> entry : commands.entrySet()){
            Command c = entry.getKey();
            List<String> args = Arrays.asList(c.aliases());
            if(entry.getKey().name().equalsIgnoreCase(command))
                return Optional.of(entry.getValue());
            else if(args.contains(command.toLowerCase(Locale.ROOT)))
                return Optional.of(entry.getValue());

        }
        return Optional.empty();
    }

}
