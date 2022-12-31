package test;

import lombok.SneakyThrows;
import org.carboncock.metagram.filters.Filters;
import org.carboncock.metagram.telegram.api.MetaGramApi;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


public class Main {

    private static final String TOKEN = "1492362467:AAE6SwBRUElBwwxIYgenhiCVPcdFcU-VVHg";
    private static final String USERNAME = "CryptoTestRobot";

    @SneakyThrows
    public static void main(String[] args) {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class); // TelegramBots api rubenlaugs
        MetaGramApi bot = new MetaGramApi(); // MetaGram api CarbonCock
        bot.setBotToken(TOKEN);
        bot.setBotUsername(USERNAME);

        api.registerBot(bot); // TelegramBots api
        bot.registerEvents("test");
    }
}
