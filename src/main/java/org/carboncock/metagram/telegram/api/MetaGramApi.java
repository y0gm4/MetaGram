package org.carboncock.metagram.telegram.api;

import lombok.Setter;
import lombok.SneakyThrows;
import org.carboncock.metagram.annotations.*;
import org.carboncock.metagram.exceptions.*;
import org.carboncock.metagram.annotations.types.SendMethod;
import org.carboncock.metagram.filters.*;
import org.carboncock.metagram.listeners.CallbackListener;
import org.carboncock.metagram.listeners.CommandListener;
import org.carboncock.metagram.listeners.Listener;
import org.carboncock.metagram.listeners.UpdateListener;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MetaGramApi extends TelegramLongPollingBot {

    protected static final Map<Callback, Class<? extends CallbackListener>> query = new HashMap<>();
    protected static final Map<Command, Class<? extends CommandListener>> commands = new HashMap<>();
    protected static final List<UpdateListener> listeners = new ArrayList<>();
    protected static final List<Listener> genericListeners = new ArrayList<>();
    protected static final Map<Method, List<Annotation>> annotationsPerMethod = new HashMap<>();

    @SneakyThrows
    public MetaGramApi(){
        registerEvent(new CallbackHandler());
        registerEvent(new CommandHandler());
    }

    @Setter
    private String botUsername;

    @Setter
    private String botToken;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        listeners.forEach(updateListener -> updateListener.onUpdate(this, update));
        annotationsPerMethod.forEach((method, annotations) -> {
            boolean shouldMethodExec = false;
            for(Annotation annotation : annotations){
                if(annotation.annotationType().equals(Filters.class)){
                    Filters filters = (Filters) annotation;
                    try {
                        shouldMethodExec |= FilterType.checkFilters(
                                filters.value(),
                                filters.fromWhere(),
                                filters.fromWho(),
                                filters.regex(),
                                this,
                                update
                        );
                    } catch (BadFilterTypeException | TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
                else if(annotation.annotationType().equals(CustomFilter.class)){
                    CustomFilter customFilter = (CustomFilter) annotation;
                    Class<?> loc = customFilter.cfLocation().equals(Class.class) ? method.getDeclaringClass() :  customFilter.cfLocation();
                    String id = customFilter.value();
                    Optional<Method> methodOpt = Arrays.stream(loc.getMethods())
                            .filter(method1 -> method1.isAnnotationPresent(CustomFilterIdentifier.class) && method1.getAnnotation(CustomFilterIdentifier.class).value().equalsIgnoreCase(id))
                            .findFirst();
                    if(!methodOpt.isPresent()) continue;
                    Method m = methodOpt.get();
                    try {
                        shouldMethodExec |= (boolean) m.invoke(m.getDeclaringClass().getDeclaredConstructor().newInstance(), this, update);
                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(shouldMethodExec)
                try {
                    method.invoke(method.getDeclaringClass().getDeclaredConstructor().newInstance(), this, update);
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
        });
    }

    @SneakyThrows
    public void registerEvent(Listener event) {
        if(event instanceof CommandListener){
            Class<? extends CommandListener> clazz = ((CommandListener) event).getClass();
            if(!clazz.isAnnotationPresent(Command.class))
                throw new AnnotationMissingException(String.format("Command.class annotation is missing for %s class", clazz.getName()));
            Command command = clazz.getAnnotation(Command.class);
            commands.put(command, clazz);

            if(clazz.isAnnotationPresent(Permission.class)){
                Permission permission = clazz.getAnnotation(Permission.class);
                if(permission.send().equals(SendMethod.ANSWER_CALLBACK_QUERY))
                    throw new IllegalSendingMethodException("You cannot respond to a command with AnswerCallbackQuery.class");
                if(permission.send().equals(SendMethod.EDIT_MESSAGE))
                    throw new IllegalSendingMethodException("You can only edit your own messages!");
                try {
                    Class<?> locClass = permission.listLocation();
                    Optional<Field> optField = Arrays.stream(locClass.getDeclaredFields())
                            .filter(field -> field.isAnnotationPresent(PermissionHandler.class))
                            .findFirst();
                    Field field = optField.get();
                    field.setAccessible(true);
                    List<Long> list = (List<Long>) field.get(locClass.getDeclaredConstructor().newInstance());
                }catch(Exception e){
                    throw new ListFieldNotFoundException(
                            String.format("The field of type List<Long> was not found/can't be accessed!\nclass: %s", clazz.getName())
                    );
                }
            }
        }
        else if(event instanceof CallbackListener){
            Class<? extends CallbackListener> clazz = ((CallbackListener) event).getClass();
            if(!clazz.isAnnotationPresent(Callback.class))
                throw new AnnotationMissingException(String.format("Callback.class annotation is missing for %s class", clazz.getName()));
            Callback callback = clazz.getAnnotation(Callback.class);
            query.put(callback, clazz);
            if(clazz.isAnnotationPresent(Permission.class))
                try {
                    Permission permission = clazz.getAnnotation(Permission.class);
                    Class<?> locClass = permission.listLocation();
                    Optional<Field> optField = Arrays.stream(locClass.getDeclaredFields())
                            .filter(field -> field.isAnnotationPresent(PermissionHandler.class))
                            .findFirst();
                    Field field = optField.get();
                    field.setAccessible(true);
                    List<Long> list = (List<Long>) field.get(locClass.getDeclaredConstructor().newInstance());
                }catch(Exception e){
                    throw new ListFieldNotFoundException(
                            String.format("The field of type List<Long> was not found/can't be accessed!\nclass: %s", clazz.getName())
                    );
                }
        }
        else if(event instanceof UpdateListener){
            listeners.add((UpdateListener) event);
        }
        else {
            Class<? extends Listener> clazz = event.getClass();
            if(!clazz.isAnnotationPresent(EventHandler.class))
                throw new AnnotationMissingException(String.format("EventHandler.class annotation is missing for class %s", clazz.getName()));
            Arrays.stream(clazz.getMethods())
                    .filter(method -> (method.isAnnotationPresent(Filters.class) || method.isAnnotationPresent(CustomFilters.class))
                            && method.getParameterTypes().length == 2)
                    .forEach(method -> {
                        List<Annotation> annotations = new ArrayList<>();
                        if(!method.getParameterTypes()[0].equals(TelegramLongPollingBot.class) || !method.getParameterTypes()[1].equals(Update.class))
                            try {
                                throw new IllegalMethodException("The annotated method must have as its first and second parameters the respective types: TelegramLongPollingBot.class & Update.class");
                            } catch (IllegalMethodException e) {
                                e.printStackTrace();
                                return;
                            }
                        for(CustomFilter customFilter : method.getAnnotationsByType(CustomFilter.class)){
                            Class<?> loc = customFilter.cfLocation().equals(Class.class) ? clazz :  customFilter.cfLocation();
                            String id = customFilter.value();
                            Arrays.stream(loc.getMethods())
                                    .filter(method1 -> method1.isAnnotationPresent(CustomFilterIdentifier.class) && method1.getAnnotation(CustomFilterIdentifier.class).value().equalsIgnoreCase(id))
                                    .findFirst()
                                    .ifPresent(method1 -> {
                                        if(!method1.isAnnotationPresent(CustomFilterIdentifier.class) || method1.getParameters().length != 2 || !method1.getReturnType().equals(boolean.class) ||
                                                method1.getParameterTypes()[0] != TelegramLongPollingBot.class || method1.getParameterTypes()[1] != Update.class)
                                            try {
                                                throw new IllegalMethodException("Error, method must be annotated with 'CustomFilterIdentifier.class', have 'TelegramLongPollingBot.class' and 'Update.class' as parameters, and a boolean as return type");
                                            } catch (IllegalMethodException e) {
                                                e.printStackTrace();
                                                return;
                                            }

                                        annotations.add(customFilter);
                                    });
                        }
                        annotations.addAll(Arrays.asList(method.getAnnotationsByType(Filters.class)));
//                        System.out.println(annotations);
//                        System.out.println(method);
                        annotationsPerMethod.put(method, annotations);
                    });
            genericListeners.add(event);
        }
    }

    @SneakyThrows
    public void registerEvents(String packagePath){
        new Reflections(packagePath, new SubTypesScanner(false))
                .getSubTypesOf(Object.class)
                .stream()
                .filter(clazz -> clazz.isAnnotationPresent(Command.class) ||
                        clazz.isAnnotationPresent(Callback.class) ||
                        clazz.isAnnotationPresent(EventHandler.class) ||
                        Listener.class.isAssignableFrom(clazz))
                .map(clazz -> {
                    Listener listener = null;
                    try {
                        listener = (Listener) clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return listener;
                })
                .forEach(this::registerEvent);
    }

}
