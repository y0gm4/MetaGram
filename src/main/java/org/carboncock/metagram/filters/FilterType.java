package org.carboncock.metagram.filters;

import lombok.ToString;
import org.carboncock.metagram.exceptions.BadFilterTypeException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToString
public enum FilterType {
    MESSAGE, COMMAND, CALLBACK_QUERY,
    EDIT_MESSAGE, REPLY_MESSAGE, FORWARD_MESSAGE,
    POLL, POLL_ANSWER, JOIN_REQUEST,
    INLINE_QUERY, CHOSEN_INLINE_QUERY, PRE_CHECKOUT_QUERY, SHIPPING_QUERY,
    CHAT_MEMBER_UPDATE,
    PHOTO, VIDEO, DOCUMENT, ANIMATION, CONTACT, DICE, INVOICE, AUDIO,
    LOCATION, REPLY_MARKUP, STICKER, ANIMATED_STICKER;

    public interface By extends FilterModifiers {
        String BOT = "BOT";
        String USER = "USER";
        String EVERYONE = "EVERYONE";
        String ADMIN = "ADMIN";
        String CREATOR = "CREATOR";
    }

    private interface FilterModifiers{}

    public interface Chat extends FilterModifiers {
        String PRIVATE = "PRIVATE";
        String CHANNEL = "CHANNEL";
        String GROUP = "GROUP";
        String SUPER_GROUP = "SUPER_GROUP";
        String EVERYWHERE = "EVERYWHERE";
    }

    private static <F extends FilterModifiers> void checkForBadFilters(String[] filters, Class<F> fType) throws BadFilterTypeException {
        List<String> fList = Arrays.stream(
                fType.getFields())
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(fType).toString();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return "";
                })
                .collect(Collectors.toList());
        for(String filter : filters){
            if(!fList.contains(filter))
                throw new BadFilterTypeException(String.format("Error, you must use the fields in the FilterType.%s.class interface.", fType.equals(Chat.class) ? "Class" : "By"));
        }
    }

    private static boolean checkRegex(String input, String regex){
        return regex.equals("") || input.matches(regex);
    }

    public static boolean checkFilters(FilterType[] filters, String[] chats, String[] usersType, String optRegex, TelegramLongPollingBot bot, Update update) throws BadFilterTypeException, TelegramApiException {
        boolean filterResult = false, chatResult = false, userResult = false;

        checkForBadFilters(chats, Chat.class);
        checkForBadFilters(usersType, By.class);

        for(FilterType filter : filters){
                switch(filter){
                    case MESSAGE:
                        filterResult |= update.hasMessage() && update.getMessage().hasText() && checkRegex(update.getMessage().getText(), optRegex);
                        break;
                    case EDIT_MESSAGE:
                        filterResult |= update.hasEditedMessage() || (update.getEditedMessage().hasText() && checkRegex(update.getEditedMessage().getText(), optRegex));
                        break;
                    case REPLY_MESSAGE:
                        filterResult |= update.hasMessage() && update.getMessage().isReply() && checkRegex(update.getMessage().getText(), optRegex);
                        break;
                    case FORWARD_MESSAGE:
                        filterResult |= update.hasMessage() && update.getMessage().getForwardFrom() != null && checkRegex(update.getMessage().getText(), optRegex);
                        break;
                    case PHOTO:
                        filterResult |= update.hasMessage() && update.getMessage().hasPhoto() || checkRegex(update.getMessage().getCaption(), optRegex);
                        break;
                    case VIDEO:
                        filterResult |= update.hasMessage() && update.getMessage().hasVideo() || checkRegex(update.getMessage().getCaption(), optRegex);
                        break;
                    case DOCUMENT:
                        filterResult |= update.hasMessage() && update.getMessage().hasDocument();
                        break;
                    case ANIMATION:
                        filterResult |= update.hasMessage() && update.getMessage().hasAnimation();
                        break;
                    case AUDIO:
                        filterResult |= update.hasMessage() && update.getMessage().hasAudio();
                        break;
                    case CONTACT:
                        filterResult |= update.hasMessage() && update.getMessage().hasContact();
                        break;
                    case DICE:
                        filterResult |= update.hasMessage() && update.getMessage().hasDice();
                        break;
                    case INVOICE:
                        filterResult |= update.hasMessage() && update.getMessage().hasInvoice();
                        break;
                    case LOCATION:
                        filterResult |= update.hasMessage() && update.getMessage().hasLocation();
                        break;
                    case REPLY_MARKUP:
                        filterResult |= update.hasMessage() && update.getMessage().hasReplyMarkup() || checkRegex(update.getMessage().getCaption(), optRegex);
                        break;
                    case STICKER:
                        filterResult |= update.hasMessage() && update.getMessage().hasSticker();
                        break;
                    case ANIMATED_STICKER:
                        filterResult |= update.hasMessage() && update.getMessage().hasSticker() && update.getMessage().getSticker().getIsAnimated();
                        break;
                    case POLL:
                        filterResult |= update.hasMessage() && update.getMessage().hasPoll();
                        break;
                    case CALLBACK_QUERY:
                        filterResult |= update.hasCallbackQuery();
                        break;
                    case POLL_ANSWER:
                        filterResult |= update.hasPollAnswer();
                        break;
                    case JOIN_REQUEST:
                        filterResult |= update.hasChatJoinRequest();
                        break;
                    case INLINE_QUERY:
                        filterResult |= update.hasInlineQuery();
                        break;
                    case CHOSEN_INLINE_QUERY:
                        filterResult |= update.hasChosenInlineQuery();
                        break;
                    case PRE_CHECKOUT_QUERY:
                        filterResult |= update.hasPreCheckoutQuery();
                        break;
                    case SHIPPING_QUERY:
                        filterResult |= update.hasShippingQuery();
                        break;
                    case CHAT_MEMBER_UPDATE:
                        filterResult |= update.hasChatMember();
                        break;
                }
        }
        Map<String, Object> res = getUserByUpdate(update);
        long userId = ((User) res.get("user")).getId();
        String chatId = (String) res.getOrDefault("chat","");

        if(!Arrays.asList(chats).contains(Chat.EVERYWHERE) && !chatId.equals("")){
            org.telegram.telegrambots.meta.api.objects.Chat chat = bot.execute(
                    GetChat.builder()
                            .chatId(chatId)
                            .build()
            );
            for(String c : chats){
                switch(c){
                    case Chat.CHANNEL:
                        chatResult |= chat.isChannelChat();
                        break;
                    case Chat.GROUP:
                        chatResult |= chat.isGroupChat();
                        break;
                    case Chat.SUPER_GROUP:
                        chatResult |= chat.isSuperGroupChat();
                        break;
                    case Chat.PRIVATE:
                        chatResult |= chat.isUserChat();
                        break;
                }
            }
        }
        else
            chatResult = true;

        if(!Arrays.asList(usersType).contains(By.EVERYONE) && !chatId.equals("")){

            ChatMember chatMember = bot.execute(
                    GetChatMember.builder()
                            .userId(userId)
                            .chatId(chatId)
                            .build()
            );
            org.telegram.telegrambots.meta.api.objects.Chat chat = bot.execute(
                    GetChat.builder()
                            .chatId(chatId)
                            .build()
            );
            if(chat.isGroupChat() || chat.isSuperGroupChat())
                for(String userType : usersType){
                    switch(userType){
                        case By.BOT:
                            userResult |= ((User) res.get("user")).getIsBot();
                            break;
                        case By.ADMIN:
                            userResult |= chatMember.getStatus().equalsIgnoreCase("administrator");
                            break;
                        case By.CREATOR:
                            userResult |= chatMember.getStatus().equalsIgnoreCase("creator");
                            break;
                        case By.USER:
                            userResult |= (!chatMember.getStatus().equalsIgnoreCase("administrator") && !chatMember.getStatus().equalsIgnoreCase("creator"));
                    }
                }
            else
                userResult = true;
        }
        else
            userResult = true;
        return filterResult && chatResult && userResult;
    }

    private static Map<String, Object> getUserByUpdate(Update update){
        Map<String, Object> map = new HashMap<>();
        if(update.hasMessage()){
            map.put("user", update.getMessage().getFrom());
            map.put("chat", update.getMessage().getChatId().toString());
        }
        else if(update.hasChannelPost()){
            map.put("user", update.getChannelPost().getFrom());
            map.put("chat", update.getChannelPost().getChatId().toString());
        }
        else if(update.hasEditedMessage()){
            map.put("user", update.getEditedMessage().getFrom());
            map.put("chat", update.getEditedMessage().getChatId().toString());
        }
        else if(update.hasEditedChannelPost()){
            map.put("user", update.getEditedChannelPost().getFrom());
            map.put("chat", update.getEditedChannelPost().getChatId().toString());
        }
        else if(update.hasChatMember()){
            map.put("user", update.getChatMember().getFrom());
            map.put("chat", update.getChatMember().getChat().getId().toString());
        }
        else if(update.hasCallbackQuery()){
            map.put("user", update.getCallbackQuery().getFrom());
            map.put("chat", update.getCallbackQuery().getMessage().getChat().getId().toString());
        }
        else if(update.hasChatJoinRequest()) {
            map.put("user", update.getChatJoinRequest().getUser());
            map.put("chat", update.getChatJoinRequest().getChat().getId().toString());
        }
        else if(update.hasChosenInlineQuery())
            map.put("user", update.getChosenInlineQuery().getFrom()); // just ignore the By & Location filters
        else if(update.hasInlineQuery())
            map.put("user", update.getInlineQuery().getFrom()); // just ignore the By & Location filters
        else if(update.hasPollAnswer())
            map.put("user", update.getPollAnswer().getUser()); // just ignore the By & Location filters
        else if(update.hasPreCheckoutQuery())
            map.put("user", update.getPreCheckoutQuery().getFrom()); // just ignore the By & Location filters
        else if(update.hasShippingQuery())
            map.put("user", update.getShippingQuery().getFrom()); // just ignore the By & Location filters


        return map;
    }

}
