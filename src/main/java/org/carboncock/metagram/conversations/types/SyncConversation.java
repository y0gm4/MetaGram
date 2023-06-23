package org.carboncock.metagram.conversations.types;

import org.carboncock.metagram.conversations.Conversation;
import org.carboncock.metagram.conversations.ConversationTemplate;

import java.util.function.Function;

public class SyncConversation<T extends ConversationTemplate> extends Conversation<T> {
    @Override
    protected SyncConversation<T> id(long id) {
        return null;
    }

    @Override
    protected SyncConversation<T> template(T template) {
        return null;
    }

    @Override
    protected SyncConversation<T> onClose(Function<T, String> func) {
        return null;
    }

    @Override
    protected SyncConversation<T> onEverySubmit(Function<T, String> func) {
        return null;
    }

    @Override
    protected SyncConversation<T> onSpecificSubmit(Function<T, String> func, int index) {
        return null;
    }

    @Override
    protected SyncConversation<T> placeHolder(int index, String... placeholders) {
        return null;
    }

    @Override
    protected SyncConversation<T> closeOnCallback(String callback, String text) {
        return null;
    }

    @Override
    protected SyncConversation<T> closeOnCommand(String command, String text) {
        return null;
    }

    @Override
    protected SyncConversation<T> build() {
        return null;
    }
    // TODO add bad answer method
}
