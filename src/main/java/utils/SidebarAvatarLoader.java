package utils;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import models.User;

/**
 * Loads the user's avatar photo into the sidebar ImageView.
 * Falls back to initials if no photo is set.
 */
public final class SidebarAvatarLoader {

    private SidebarAvatarLoader() {}

    public static void load(User user, ImageView avatarView, Label initialsLabel) {
        if (user == null) return;

        String photo = user.getPhoto();
        if (photo != null && !photo.isBlank()) {
            // Apply circular clip
            Circle clip = new Circle(36, 36, 36);
            avatarView.setClip(clip);

            new Thread(() -> {
                try {
                    Image img = new Image(photo, 72, 72, true, true, false);
                    Platform.runLater(() -> {
                        avatarView.setImage(img);
                        avatarView.setVisible(true);
                        avatarView.setManaged(true);
                        if (initialsLabel != null) {
                            initialsLabel.setVisible(false);
                            initialsLabel.setManaged(false);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showInitials(user, avatarView, initialsLabel));
                }
            }, "sidebar-avatar-loader").start();
        } else {
            showInitials(user, avatarView, initialsLabel);
        }
    }

    private static void showInitials(User user, ImageView avatarView, Label initialsLabel) {
        if (avatarView != null) { avatarView.setVisible(false); avatarView.setManaged(false); }
        if (initialsLabel != null) {
            initialsLabel.setText(initials(user));
            initialsLabel.setVisible(true);
            initialsLabel.setManaged(true);
        }
    }

    private static String initials(User u) {
        String f = u.getFirstname() != null ? u.getFirstname().trim() : "";
        String l = u.getLastname()  != null ? u.getLastname().trim()  : "";
        StringBuilder sb = new StringBuilder();
        if (!f.isEmpty()) sb.append(Character.toUpperCase(f.charAt(0)));
        if (!l.isEmpty()) sb.append(Character.toUpperCase(l.charAt(0)));
        return sb.length() > 0 ? sb.toString() : "?";
    }
}
