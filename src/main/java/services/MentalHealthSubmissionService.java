package services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.MentalHealthAssessmentSubmission;
import models.MentalHealthEvaluation;
import models.User;

import java.util.List;
import java.util.UUID;

/**
 * Feed of member mental check-ins for the coach dashboard (in-memory).
 */
public final class MentalHealthSubmissionService {

    private static final MentalHealthSubmissionService INSTANCE = new MentalHealthSubmissionService();

    private final ObservableList<MentalHealthAssessmentSubmission> submissions = FXCollections.observableArrayList();

    private MentalHealthSubmissionService() {
    }

    public static MentalHealthSubmissionService getInstance() {
        return INSTANCE;
    }

    public ObservableList<MentalHealthAssessmentSubmission> getSubmissions() {
        return submissions;
    }

    /**
     * Member removes coach recommendation from their own assessment row (in-memory).
     */
    public boolean clearRecommendationForUser(UUID submissionId, UUID userId) {
        if (submissionId == null || userId == null) {
            return false;
        }
        for (MentalHealthAssessmentSubmission s : submissions) {
            if (submissionId.equals(s.getId()) && userId.equals(s.getUserId())) {
                s.clearRecommendationContent();
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a member saves a check-in (new or updated row).
     */
    public void recordMemberSubmission(User user, MentalHealthEvaluation ev) {
        if (user == null || user.getId() == null || ev == null) {
            return;
        }
        MentalHealthAssessmentSubmission s = new MentalHealthAssessmentSubmission(UUID.randomUUID());
        s.setUserId(user.getId());
        s.setUserFullName(buildFullName(user));
        s.setUserEmail(safe(user.getEmail()));
        s.setTestedAt(ev.getTestedAt());
        s.setScore(ev.getScore());
        s.setStatus(safe(ev.getStatus()));
        s.setMemberNotes(ev.getNotes());
        s.setCoachTestTitle(ev.getCoachTestTitle());
        List<Integer> qs = ev.getQuestionScores();
        if (!qs.isEmpty()) {
            s.setMood(qs.size() > 0 ? qs.get(0) : 0);
            s.setStress(qs.size() > 1 ? qs.get(1) : 0);
            s.setSleep(qs.size() > 2 ? qs.get(2) : 0);
            s.setMotivation(qs.size() > 3 ? qs.get(3) : 0);
            s.setMentalTired(qs.size() > 4 ? qs.get(4) : 0);
        } else {
            s.setMood(ev.getMood());
            s.setStress(ev.getStress());
            s.setSleep(ev.getSleep());
            s.setMotivation(ev.getMotivation());
            s.setMentalTired(ev.getMentalTired());
        }
        submissions.add(0, s);
    }

    private static String buildFullName(User u) {
        String f = safe(u.getFirstname()).trim();
        String l = safe(u.getLastname()).trim();
        String n = (f + " " + l).trim();
        return n.isEmpty() ? safe(u.getEmail()) : n;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
