package lavaplayer.demo;

import net.dv8tion.jda.api.entities.Message;

import java.util.function.Consumer;

public interface MessageDispatcher {
    void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure);

    void sendMessage(String message);
}
