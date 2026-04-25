package utils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory activity tracker — no DB, resets on logout.
 * Tracks user actions during the current session only.
 */
public class ActivityTracker {

    public enum EventType {
        LOGIN, LOGOUT,
        VIEW_WORKOUT, START_EXERCISE, COMPLETE_EXERCISE,
        COMPLETE_WORKOUT, VIEW_LIBRARY
    }

    public static class Event {
        public final LocalDateTime time;
        public final EventType type;
        public final String label;
        public final String detail;

        public Event(EventType type, String label, String detail) {
            this.time   = LocalDateTime.now();
            this.type   = type;
            this.label  = label;
            this.detail = detail;
        }

        public String formattedTime() {
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
    }

    private static final List<Event> events = new ArrayList<>();
    private static LocalDateTime sessionStart = null;

    public static void reset() {
        events.clear();
        sessionStart = null;
    }

    public static void startSession() {
        reset();
        sessionStart = LocalDateTime.now();
        track(EventType.LOGIN, "Session started", "Connected to FitSense");
    }

    public static void track(EventType type, String label, String detail) {
        events.add(new Event(type, label, detail));
    }

    public static List<Event> getEvents() {
        return List.copyOf(events);
    }

    public static boolean hasActivity() {
        return events.stream().anyMatch(e -> e.type != EventType.LOGIN);
    }

    // ── Analysis ──────────────────────────────────────────

    public static int totalActiveMinutes() {
        if (sessionStart == null) return 0;
        long exerciseEvents = events.stream()
                .filter(e -> e.type == EventType.COMPLETE_EXERCISE)
                .count();
        // Estimate: each completed exercise ≈ avg duration
        return (int)(exerciseEvents * 5); // rough estimate
    }

    public static int completedExercisesCount() {
        return (int) events.stream()
                .filter(e -> e.type == EventType.COMPLETE_EXERCISE)
                .count();
    }

    public static int completedWorkoutsCount() {
        return (int) events.stream()
                .filter(e -> e.type == EventType.COMPLETE_WORKOUT)
                .count();
    }

    public static String behaviorInsight() {
        int total = events.size();
        if (total <= 1) return null;

        int hour = LocalTime.now().getHour();
        String timeOfDay = hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening";

        int exercises = completedExercisesCount();
        int workouts  = completedWorkoutsCount();

        if (exercises == 0 && workouts == 0) {
            return "You browsed workouts but haven't started any exercise yet.";
        }
        if (workouts > 0) {
            return "Great job! You completed " + workouts + " workout(s) this " + timeOfDay + ".";
        }
        if (exercises > 0) {
            return "You completed " + exercises + " exercise(s) this " + timeOfDay + ". Keep going!";
        }
        return "You're active this " + timeOfDay + ". Start an exercise to track progress!";
    }

    public static String icon(EventType type) {
        return switch (type) {
            case LOGIN            -> "🔑";
            case LOGOUT           -> "👋";
            case VIEW_WORKOUT     -> "👁";
            case START_EXERCISE   -> "▶";
            case COMPLETE_EXERCISE-> "✔";
            case COMPLETE_WORKOUT -> "🏆";
            case VIEW_LIBRARY     -> "📚";
        };
    }
}
