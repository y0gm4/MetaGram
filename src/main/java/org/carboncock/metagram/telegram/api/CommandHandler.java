package org.carboncock.metagram.telegram.api;

import lombok.SneakyThrows;
import org.carboncock.metagram.annotations.Command;
import org.carboncock.metagram.annotations.HelpIdentifier;
import org.carboncock.metagram.annotations.Permission;
import org.carboncock.metagram.annotations.PermissionHandler;
import org.carboncock.metagram.exceptions.IllegalMethodException;
import org.carboncock.metagram.exceptions.IllegalPermissionHandlerMethodException;
import org.carboncock.metagram.annotations.types.PermissionType;
import org.carboncock.metagram.annotations.types.SendMethod;
import org.carboncock.metagram.listeners.CommandListener;
import org.carboncock.metagram.listeners.Listener;
import org.carboncock.metagram.listeners.Permissionable;
import org.carboncock.metagram.listeners.UpdateListener;
import org.carboncock.metagram.telegram.data.CommandData;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CommandHandler implements UpdateListener {

    private final Map<Command, Class<? extends CommandListener>> commands = MetaGramApi.commands;
    private final List<Listener> genericListener = MetaGramApi.genericListeners;

    protected CommandHandler(){}

    @SneakyThrows
    @Override
    public void onUpdate(TelegramLongPollingBot bot, Update update) {
        if(!update.hasMessage()) return;
        if(!update.getMessage().hasText()) return;

        char prefix = update.getMessage().getText().charAt(0);
        String command = update.getMessage().getText().substring(1);
        String headCommand = command;
        if(command.split(" ").length > 1) headCommand = command.substring(0, command.indexOf(" "));
        String finalHeadCommand1 = headCommand;
        getCommandMethod(headCommand, prefix).ifPresent(method -> {
            if(method.getParameterTypes().length != 1 && (method.getParameterTypes()[0].equals(CommandData.class)))
                try {
                    throw new IllegalMethodException("The annotated method must have only 1 parameter of type CommandData.class");
                } catch (IllegalMethodException e) {
                    e.printStackTrace();
                    return;
                }
            Class<?> mClass = method.getDeclaringClass();
            if(method.isAnnotationPresent(Permission.class) && !isPermitted(method.getAnnotation(Permission.class), mClass, bot, update)) return;
            CommandData commandData = new CommandData(
                    command.replace(finalHeadCommand1, "").trim().split(" "),
                    update.getMessage().getFrom(),
                    Optional.ofNullable(update.getMessage().getReplyToMessage()),
                    bot,
                    update
            );
            commandData.setAnnotation(method.getAnnotation(Command.class));
            commandData.setPermission(Optional.ofNullable(method.getAnnotation(Permission.class)));
            try {
                method.invoke(mClass.getDeclaredConstructor().newInstance(), commandData);
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        if(headCommand.equalsIgnoreCase("help") && !command.equalsIgnoreCase("help")){
            String cmd = command.replace(headCommand, "").trim();
            Optional<Method> commandHelpMethodOpt = getCommandMethod(cmd, prefix);
            commandHelpMethodOpt.ifPresent(method -> {
                if(!method.isAnnotationPresent(HelpIdentifier.class))
                    return;
                HelpIdentifier helpId = method.getAnnotation(HelpIdentifier.class);
                Class<?> clazz = method.getDeclaringClass();
                Arrays.stream(clazz.getMethods())
                        .filter(m -> m.isAnnotationPresent(HelpIdentifier.class) && m.getAnnotation(HelpIdentifier.class).value().equalsIgnoreCase(helpId.value()))
                        .filter(m -> !m.equals(method))
                        .findFirst()
                        .ifPresent(m -> {
                            if(m.getParameterTypes().length != 2 && (m.getParameterTypes()[0].equals(TelegramLongPollingBot.class) || m.getParameterTypes()[1].equals(Update.class)))
                                try {
                                    throw new IllegalMethodException("The annotated method must have as its first and second parameters the respective types: TelegramLongPollingBot.class & Update.class");
                                } catch (IllegalMethodException e) {
                                    e.printStackTrace();
                                    return;
                                }

                            try {
                                m.invoke(m.getDeclaringClass().getDeclaredConstructor().newInstance(), bot, update);
                            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        });
            });
            if(commandHelpMethodOpt.isPresent()) return;
            Optional<Class<? extends CommandListener>> optClass = getCommandClass(cmd, prefix);
            if(!optClass.isPresent()) return;
            Class<? extends CommandListener> clazz = optClass.get();
            Method m = clazz.getMethod("onHelpCommand", TelegramLongPollingBot.class, Update.class);
            m.invoke(clazz.getDeclaredConstructor().newInstance(), bot, update);
            return;
        }

        Optional<Class<? extends CommandListener>> optClass = getCommandClass(headCommand, prefix);
        if(!optClass.isPresent()) return;

        Class<? extends CommandListener> clazz = optClass.get();
        Command c = clazz.getAnnotation(Command.class);

        if(c.checkedArgs() && c.args() != command.split(" ").length - 1) return;

        if(clazz.isAnnotationPresent(Permission.class) && !isPermitted(clazz.getAnnotation(Permission.class), clazz, bot, update))
            return;
        CommandData commandData = new CommandData(
                command.replace(headCommand, "").trim().split(" "),
                update.getMessage().getFrom(),
                Optional.ofNullable(update.getMessage().getReplyToMessage()),
                bot,
                update
        );
        commandData.setAnnotation(c);
        commandData.setPermission(Optional.ofNullable(clazz.getAnnotation(Permission.class)));
        Method m = clazz.getMethod("onCommand", CommandData.class);
        m.invoke(clazz.getDeclaredConstructor().newInstance(), commandData);

    }

    private Optional<Class<? extends CommandListener>> getCommandClass(String command, char prefix) {
        for(Map.Entry<Command, Class<? extends CommandListener>> entry : commands.entrySet()){
            Command c = entry.getKey();
            if(c.prefix() != prefix) continue;
            List<String> args = Arrays.asList(c.aliases());
            if(entry.getKey().value().equalsIgnoreCase(command))
                return Optional.of(entry.getValue());
            else if(args.contains(command.toLowerCase(Locale.ROOT)))
                return Optional.of(entry.getValue());

        }
        return Optional.empty();
    }

    private Optional<Method> getCommandMethod(String command, char prefix) {
        AtomicReference<Optional<Method>> method = new AtomicReference<>(Optional.empty());
        genericListener.forEach(listener -> {
            Class<? extends Listener> clazz = listener.getClass();
            Optional<Method> optMethod = Arrays.stream(clazz.getMethods())
                    .filter(m -> m.isAnnotationPresent(Command.class))
                    .filter(m -> {
                        Command c = m.getAnnotation(Command.class);
                        if (c.prefix() != prefix) return false;
                        List<String> args = Arrays.asList(c.aliases());
                        return (c.value().equalsIgnoreCase(command) || args.contains(command.toLowerCase(Locale.ROOT)))
                                && c.checkedArgs() && c.args() == command.split(" ").length - 1;
                    }).findFirst();

            if(optMethod.isPresent() && !method.get().isPresent())
                method.set(optMethod);
        });
        return method.get();
    }

    @SneakyThrows
    private boolean isPermitted(Permission permission, Class<?> clazz,  TelegramLongPollingBot bot, Update update){
        User user = update.getMessage().getFrom();
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
            SendMethod send = permission.send();
            if(send.equals(SendMethod.NONE)) return false;
            String permMissing;
            if(Permissionable.class.isAssignableFrom(clazz))
                permMissing = ((Permissionable) clazz.getDeclaredConstructor().newInstance()).onPermissionMissing();
            else if(permission.onMissingPermission().equals("") && !permission.send().equals(SendMethod.NONE))
                throw new IllegalStateException("Missing \"onMissingPermission\" value for Permission.class annotation");
            else
                permMissing = permission.onMissingPermission();
            SendMessage mex = new SendMessage();
            mex.setText(permMissing);
            mex.enableHtml(true);
            mex.setChatId("" + update.getMessage().getChatId());
            if(send.equals(SendMethod.REPLY_MESSAGE))
                mex.setReplyToMessageId(update.getMessage().getMessageId());
            bot.execute(mex);
            return false;
        }
        return true;
    }
}


