INSERT INTO feedback_response (user_id, workout_id, coach_id, rating, comment, created_at)
SELECT 
    q.user_id,
    COALESCE(qw.workout_id, 1),
    q.coach_id,
    q.exercices_compris,
    q.commentaire,
    q.date_soumission
FROM questionnaire q
LEFT JOIN questionnaire_workout qw ON qw.questionnaire_id = q.id
WHERE q.type = 'response';
