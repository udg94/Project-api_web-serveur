package com.example.projectapi.websocket;

import com.example.projectapi.entities.Message;
import com.example.projectapi.record.MessageForm;
import com.example.projectapi.repository.MessageRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageHandler extends TextWebSocketHandler {

    private final JsonMapper mapper;
    private final MessageRepository messageRepository;

    public MessageHandler(JsonMapper mapper, MessageRepository messageRepository) {
        this.mapper = mapper;
        this.messageRepository =  messageRepository;
    }

    private final Map<String, WebSocketSession> clients =
            new ConcurrentHashMap<>();;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        clients.put(userId.toString(), session);

        System.out.println(username + " connecté avec ID " + userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message_in) throws Exception {

        MessageForm message = mapper.readValue(message_in.getPayload(), MessageForm.class);

        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        String user = session.getPrincipal().getName();


        MessageForm nouveauMessage = new MessageForm(
                userId,
                message.destination_id(),
                username,
                message.contenu(),
                LocalDateTime.now()
        );

        WebSocketSession destinataire = clients.get(message.destination_id().toString());

        if (destinataire != null && destinataire.isOpen()) {
            destinataire.sendMessage(new TextMessage(mapper.writeValueAsString(nouveauMessage)));
            Message save_message = new Message(userId, message.destination_id(), username, message.contenu(), LocalDateTime.now());
            messageRepository.save(save_message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        clients.remove(userId.toString(), session);
    }
}
