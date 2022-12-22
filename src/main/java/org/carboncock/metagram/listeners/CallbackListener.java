package org.carboncock.metagram.listeners;

import org.carboncock.metagram.telegram.data.CallbackData;

public interface CallbackListener extends Listener {
    void onCallback(CallbackData callbackData);
}
