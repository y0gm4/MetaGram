package org.carboncock.metagram.listener;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CallbackListener extends Listener {
    void onCallback(TelegramLongPollingBot bot, Update update);
}
