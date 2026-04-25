SET @qid = UNHEX(REPLACE(UUID(),'-',''));

INSERT INTO questionnaire (id, titre, type, options, date_soumission)
VALUES (
    @qid,
    'Post-Workout Feedback',
    'template',
    '["1 Very poor","2 Poor","3 Average","4 Good","5 Excellent"]',
    NOW()
);

-- Link to all workouts
INSERT INTO questionnaire_workout (questionnaire_id, workout_id)
SELECT @qid, id FROM workout;
