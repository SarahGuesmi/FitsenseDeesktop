package services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.MentalHealthAssessmentSubmission;
import models.MentalHealthEvaluation;
import models.User;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Feed of member mental check-ins for the coach dashboard (backed by MySQL).
 */
public final class MentalHealthSubmissionService {

    private static final MentalHealthSubmissionService INSTANCE = new MentalHealthSubmissionService();

    private final ObservableList<MentalHealthAssessmentSubmission> submissions = FXCollections.observableArrayList();
    private final MentalHealthSubmissionRepository submissionRepository = new MentalHealthSubmissionRepository();
    private final MentalHealthEvaluationRepository evaluationRepository = new MentalHealthEvaluationRepository();

    private MentalHealthSubmissionService() {
        reloadFromDatabase();
    }

    public static MentalHealthSubmissionService getInstance() {
        return INSTANCE;
    }

    public void reloadFromDatabase() {
        try {
            List<MentalHealthAssessmentSubmission> rows = submissionRepository.loadAllWithExercises();
            submissions.setAll(rows);
        } catch (SQLException e) {
            System.err.println("MentalHealthSubmissionService load failed: " + e.getMessage());
        }
    }

    public ObservableList<MentalHealthAssessmentSubmission> getSubmissions() {
        return submissions;
    }

    /**
     * Member removes coach recommendation from their own assessment row.
     */
    public boolean clearRecommendationForUser(UUID submissionId, UUID userId) {
        if (submissionId == null || userId == null) {
            return false;
        }
        for (MentalHealthAssessmentSubmission s : submissions) {
            if (submissionId.equals(s.getId()) && userId.equals(s.getUserId())) {
                s.clearRecommendationContent();
                try {
                    submissionRepository.clearCoachRecommendationFields(submissionId);
                } catch (SQLException e) {
                    System.err.println("clearRecommendationForUser persist failed: " + e.getMessage());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Coach removes a recommendation (manage dialog).
     */
    public void clearRecommendationByCoach(UUID submissionId) {
        if (submissionId == null) {
            return;
        }
        for (MentalHealthAssessmentSubmission s : submissions) {
            if (submissionId.equals(s.getId())) {
                s.clearRecommendationContent();
                try {
                    submissionRepository.clearCoachRecommendationFields(submissionId);
                } catch (SQLException e) {
                    System.err.println("clearRecommendationByCoach persist failed: " + e.getMessage());
                }
                return;
            }
        }
    }

    public void persistCoachRecommendation(MentalHealthAssessmentSubmission s) {
        if (s == null) {
            return;
        }
        try {
            submissionRepository.persistCoachRecommendation(s);
        } catch (SQLException e) {
            System.err.println("persistCoachRecommendation failed: " + e.getMessage());
        }
    }

    /**
     * Called when a member saves a check-in (new or updated row).
     */
    public void recordMemberSubmission(User user, MentalHealthEvaluation ev) {
        if (user == null || user.getId() == null || ev == null) {
            return;
        }
        try {
            evaluationRepository.upsert(ev, user.getId());
        } catch (SQLException e) {
            System.err.println("recordMemberSubmission evaluation upsert failed: " + e.getMessage());
            return;
        }

        MentalHealthAssessmentSubmission existing = findByEvaluationId(ev.getId());
        MentalHealthAssessmentSubmission s;
        if (existing != null) {
            s = existing;
        } else {
            UUID dbId = null;
            try {
                dbId = submissionRepository.findSubmissionIdByEvaluationId(ev.getId());
            } catch (SQLException e) {
                System.err.println("findSubmissionIdByEvaluationId failed: " + e.getMessage());
            }
            if (dbId != null) {
                s = new MentalHealthAssessmentSubmission(dbId);
                submissions.add(0, s);
            } else {
                s = new MentalHealthAssessmentSubmission(UUID.randomUUID());
                submissions.add(0, s);
            }
        }
        fillSnapshot(s, user, ev);
        s.setEvaluationId(ev.getId());
        try {
            submissionRepository.upsertMemberSnapshot(user, s);
        } catch (SQLException e) {
            System.err.println("recordMemberSubmission submission upsert failed: " + e.getMessage());
        }
    }

    private MentalHealthAssessmentSubmission findByEvaluationId(UUID evaluationId) {
        if (evaluationId == null) {
            return null;
        }
        for (MentalHealthAssessmentSubmission s : submissions) {
            if (evaluationId.equals(s.getEvaluationId())) {
                return s;
            }
        }
        return null;
    }

    private static void fillSnapshot(MentalHealthAssessmentSubmission s, User user, MentalHealthEvaluation ev) {
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
