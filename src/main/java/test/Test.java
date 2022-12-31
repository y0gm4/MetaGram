package test;

import org.carboncock.metagram.annotations.EventHandler;
import org.carboncock.metagram.filters.FilterType;
import org.carboncock.metagram.filters.Filters;
import org.carboncock.metagram.listeners.Listener;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@EventHandler
public class Test implements Listener {

    @Filters(value = FilterType.MESSAGE, fromWhere = FilterType.Chat.PRIVATE, regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
    public void onUpdate(TelegramLongPollingBot bot, Update update) {
        System.out.println("oof");
    }

    @Filters(value = FilterType.REPLY_MESSAGE)
    public void asd(TelegramLongPollingBot bot, Update update){
        System.out.println("asd");
    }
}
