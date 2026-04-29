package controllers;

import app.AppSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;import javafx.util.Duration;
import models.ChatMessage;
import models.User;
import services.ChatService;
import services.UserService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChatroomController {

    @FXML private VBox contactsListBox;
    @FXML private VBox emptyState;
    @FXML private VBox conversationArea;
    @FXML private Label convAvatar;
    @FXML private Label convName;
    @FXML private Label convRole;
    @FXML private ScrollPane messagesScroll;
    @FXML private VBox messagesBox;
    @FXML private TextField messageInput;

    private final UserService userService = new UserService();
    private final ChatService chatService = new ChatService();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    private User selectedContact;
    private LocalDateTime lastPollTime;
    private Timeline pollTimeline;
    private final Set<UUID> renderedMessageIds = new HashSet<>();

    @FXML
    private void initialize() {
        loadContacts();
        // Handle Enter key in message input
        messageInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                onSendMessage();
                e.consume();
            }
        });
        startPolling();
    }

    // ── Contacts ─────────────────────────────────────────────────────────────

    private void loadContacts() {
        User me = AppSession.getCurrentUser();
        if (me == null) return;
        try {
            List<User> users = new ArrayList<>(userService.read());
            users.removeIf(u -> u.getId().equals(me.getId()));

            Map<UUID, LocalDateTime> lastMsgTimes = new HashMap<>();
            for (User u : users) {
                LocalDateTime t = chatService.getLastMessageTime(me.getId(), u.getId());
                lastMsgTimes.put(u.getId(), t != null ? t : LocalDateTime.MIN);
            }
            users.sort((u1, u2) -> lastMsgTimes.get(u2.getId()).compareTo(lastMsgTimes.get(u1.getId())));

            contactsListBox.getChildren().clear();
            for (User u : users) {
                contactsListBox.getChildren().add(buildContactRow(u));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox buildContactRow(User u) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("chat-contact-row");
        row.setPadding(new Insets(10, 12, 10, 12));

        Label avatar = new Label(initials(u));
        avatar.getStyleClass().add("chat-contact-avatar");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label name = new Label(fullName(u));
        name.getStyleClass().add("chat-contact-name");
        
        int unreadCount = 0;
        try {
            unreadCount = chatService.countUnread(u.getId(), AppSession.getCurrentUser().getId());
        } catch (SQLException ignored) {}

        if (unreadCount > 0) {
            name.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            Label badge = new Label(String.valueOf(unreadCount));
            badge.getStyleClass().add("chat-unread-badge");
            
            HBox nameRow = new HBox(8);
            nameRow.setAlignment(Pos.CENTER_LEFT);
            nameRow.getChildren().addAll(name, badge);
            info.getChildren().add(nameRow);
        } else {
            info.getChildren().add(name);
        }

        Label role = new Label(roleLabel(u.getRolesJson()));
        role.getStyleClass().add("chat-contact-role");
        info.getChildren().add(role);

        row.getChildren().addAll(avatar, info);
        row.setOnMouseClicked(e -> openConversation(u));
        return row;
    }

    // ── Conversation ─────────────────────────────────────────────────────────

    private void openConversation(User contact) {
        selectedContact = contact;
        lastPollTime = null;
        renderedMessageIds.clear();
        messagesBox.getChildren().clear();

        convAvatar.setText(initials(contact));
        convName.setText(fullName(contact));
        convRole.setText(roleLabel(contact.getRolesJson()));

        // highlight selected
        contactsListBox.getChildren().forEach(n -> n.getStyleClass().remove("chat-contact-row-active"));
        contactsListBox.getChildren().stream()
                .filter(n -> n instanceof HBox)
                .map(n -> (HBox) n)
                .filter(r -> fullName(contact).equals(
                        ((Label) ((VBox) r.getChildren().get(1)).getChildren().get(0)).getText()))
                .findFirst()
                .ifPresent(r -> r.getStyleClass().add("chat-contact-row-active"));

        emptyState.setManaged(false);
        emptyState.setVisible(false);
        conversationArea.setManaged(true);
        conversationArea.setVisible(true);
        conversationArea.setMaxWidth(Double.MAX_VALUE);
        conversationArea.setMaxHeight(Double.MAX_VALUE);
        StackPane.setAlignment(conversationArea, javafx.geometry.Pos.TOP_LEFT);

        loadMessages();
        try {
            chatService.markAsRead(contact.getId(), AppSession.getCurrentUser().getId());
            loadContacts(); // Refresh list to clear red name
        } catch (SQLException ignored) {}
    }

    private void loadMessages() {
        if (selectedContact == null) return;
        User me = AppSession.getCurrentUser();
        if (me == null) return;
        try {
            List<ChatMessage> msgs = chatService.getConversation(me.getId(), selectedContact.getId());
            renderedMessageIds.clear();
            messagesBox.getChildren().clear();
            for (ChatMessage m : msgs) renderMessage(m);
            if (!msgs.isEmpty()) lastPollTime = msgs.get(msgs.size() - 1).getSentAt();
            scrollToBottom();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderMessage(ChatMessage m) {
        if (m.getId() != null && renderedMessageIds.contains(m.getId())) return;
        if (m.getId() != null) renderedMessageIds.add(m.getId());

        User me = AppSession.getCurrentUser();
        boolean isMine = me != null && me.getId().equals(m.getSenderId());

        HBox wrapper = new HBox();
        wrapper.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add(isMine ? "chat-bubble-mine" : "chat-bubble-theirs");
        bubble.setMaxWidth(420);

        Label content = new Label(m.getContent());
        content.getStyleClass().add("chat-bubble-text");
        content.setWrapText(true);

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_RIGHT);
        Label time = new Label(m.getSentAt() != null ? m.getSentAt().format(timeFmt) : "");
        time.getStyleClass().add("chat-bubble-time");
        meta.getChildren().add(time);

        if (isMine) {
            Button editBtn = new Button("✎");
            editBtn.getStyleClass().add("chat-msg-action-btn");
            editBtn.setOnAction(e -> promptEdit(m, content));

            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("chat-msg-action-btn");
            delBtn.setOnAction(e -> deleteMessage(m, wrapper));

            meta.getChildren().addAll(editBtn, delBtn);
        }

        bubble.getChildren().addAll(content, meta);
        wrapper.getChildren().add(bubble);
        messagesBox.getChildren().add(wrapper);
    }

    private void promptEdit(ChatMessage m, Label contentLabel) {
        TextInputDialog dialog = new TextInputDialog(m.getContent());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText(null);
        dialog.setContentText("Edit your message:");
        dialog.showAndWait().ifPresent(newText -> {
            if (newText.isBlank() || newText.equals(m.getContent())) return;
            try {
                chatService.edit(m.getId(), newText.trim());
                m.setContent(newText.trim());
                contentLabel.setText(newText.trim());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteMessage(ChatMessage m, HBox wrapper) {
        try {
            chatService.deleteMessage(m.getId());
            renderedMessageIds.remove(m.getId());
            messagesBox.getChildren().remove(wrapper);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onDeleteConversation() {
        if (selectedContact == null) return;
        User me = AppSession.getCurrentUser();
        if (me == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete all messages with " + fullName(selectedContact) + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    chatService.deleteConversation(me.getId(), selectedContact.getId());
                    renderedMessageIds.clear();
                    messagesBox.getChildren().clear();
                    lastPollTime = null;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onSendMessage() {
        if (selectedContact == null) return;
        User me = AppSession.getCurrentUser();
        if (me == null) return;
        String text = messageInput.getText() == null ? "" : messageInput.getText().trim();
        if (text.isEmpty()) return;
        // Clear input immediately so the UI feels responsive
        messageInput.clear();
        messageInput.setDisable(true);
        User contact = selectedContact;
        new Thread(() -> {
            try {
                ChatMessage msg = chatService.send(me.getId(), contact.getId(), text);
                Platform.runLater(() -> {
                    renderMessage(msg);
                    lastPollTime = msg.getSentAt();
                    scrollToBottom();
                    loadContacts(); // Move to top
                    messageInput.setDisable(false);
                    messageInput.requestFocus();
                });
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // Restore text so the user doesn't lose their message
                    messageInput.setText(text);
                    messageInput.setDisable(false);
                    messageInput.requestFocus();
                });
            }
        }).start();
    }

    // ── Polling ──────────────────────────────────────────────────────────────

    private void startPolling() {
        pollTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> poll()));
        pollTimeline.setCycleCount(Timeline.INDEFINITE);
        pollTimeline.play();
    }

    private void poll() {
        if (selectedContact == null || lastPollTime == null) return;
        User me = AppSession.getCurrentUser();
        if (me == null) return;
        LocalDateTime since = lastPollTime;
        new Thread(() -> {
            try {
                List<ChatMessage> newMsgs = chatService.getNewMessages(me.getId(), selectedContact.getId(), since);
                if (!newMsgs.isEmpty()) {
                    Platform.runLater(() -> {
                        for (ChatMessage m : newMsgs) renderMessage(m);
                        lastPollTime = newMsgs.get(newMsgs.size() - 1).getSentAt();
                        scrollToBottom();
                        loadContacts(); // Move to top
                    });
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public void stopPolling() {
        if (pollTimeline != null) pollTimeline.stop();
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private void scrollToBottom() {
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private static String initials(User u) {
        String f = u.getFirstname() == null ? "" : u.getFirstname().trim();
        String l = u.getLastname() == null ? "" : u.getLastname().trim();
        String init = "";
        if (!f.isEmpty()) init += Character.toUpperCase(f.charAt(0));
        if (!l.isEmpty()) init += Character.toUpperCase(l.charAt(0));
        return init.isEmpty() ? "?" : init;
    }

    private static String fullName(User u) {
        String f = u.getFirstname() == null ? "" : u.getFirstname().trim();
        String l = u.getLastname() == null ? "" : u.getLastname().trim();
        return (f + " " + l).trim();
    }

    private static String roleLabel(String rolesJson) {
        if (rolesJson == null) return "User";
        if (rolesJson.contains("ROLE_ADMIN")) return "Administrator";
        if (rolesJson.contains("ROLE_COACH")) return "Coach";
        return "User";
    }
}
