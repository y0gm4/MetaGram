package org.carboncock.metagram.listener;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CommandListener extends Listener {
    void onCommand(TelegramLongPollingBot bot, Update update);

    default void onHelpCommand(TelegramLongPollingBot bot, Update update) { }
}
