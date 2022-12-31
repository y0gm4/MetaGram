package test;

import org.carboncock.metagram.annotations.EventHandler;
import org.carboncock.metagram.filters.CustomFilter;
import org.carboncock.metagram.filters.CustomFilterIdentifier;
import org.carboncock.metagram.filters.FilterType;
import org.carboncock.metagram.filters.Filters;
import org.carboncock.metagram.listeners.Listener;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@EventHandler
public class Test2 implements Listener {

    @Filters(value = FilterType.CALLBACK_QUERY, fromWhere = FilterType.Chat.PRIVATE)
    public void azd(TelegramLongPollingBot bot, Update update){
        System.out.println("asdd");
    }

    @CustomFilter("a")
    @CustomFilter("b")
    public void asd(TelegramLongPollingBot bot, Update update){
        System.out.println("a");
    }

    @CustomFilterIdentifier("a")
    public boolean custom(TelegramLongPollingBot bot, Update update){
        System.out.println("custom");
        return true;
    }

    @CustomFilterIdentifier("b")
    public boolean custom2(TelegramLongPollingBot bot, Update update){
        System.out.println("custom2");
        return true;
    }
}
