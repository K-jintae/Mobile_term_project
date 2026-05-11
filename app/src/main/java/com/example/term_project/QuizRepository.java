package com.example.term_project;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizRepository {

    private final FirebaseFirestore db;

    private final String ROOT_COLLECTION = "subjects";
    private final String SUB_COLLECTION = "quizzes";

    public QuizRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnQuestionFetchedListener {
        void onSuccess(QuizQuestion question);
        void onFailure(Exception e);
    }

    public interface OnQuestionsFetchedListener {
        void onSuccess(List<QuizQuestion> questions);
        void onFailure(Exception e);
    }

    /*
     * 새 방식:
     * quiz_id를 1,2,3... 순서로 하나씩 찾지 않고,
     * 해당 과목의 quizzes 전체를 가져온 뒤 앱에서 필터링한다.
     *
     * 장점:
     * - quiz_id가 중간에 비어 있어도 끊기지 않음
     * - Firebase에 문제가 10개 이상 있으면 최대 10개까지 출제 가능
     * - difficulty_level 필드가 없어도 fallback으로 동작 가능
     */
    public void getQuizQuestionsFromFirestore(
            int subjectId,
            String difficultyLevel,
            int maxQuestionCount,
            OnQuestionsFetchedListener listener
    ) {
        db.collection(ROOT_COLLECTION)
                .whereEqualTo("subject_id", subjectId)
                .get()
                .addOnSuccessListener(subjectDocs -> {
                    if (!subjectDocs.isEmpty()) {
                        DocumentSnapshot subjectDoc = subjectDocs.getDocuments().get(0);
                        fetchQuizList(subjectDoc, subjectId, difficultyLevel, maxQuestionCount, listener);
                    } else {
                        tryStringSubjectId(subjectId, difficultyLevel, maxQuestionCount, listener);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(
                        new Exception("과목 데이터 읽기 실패: " + e.getMessage())
                ));
    }

    /*
     * 기존 코드 호환용 메서드.
     * 혹시 다른 파일에서 이 메서드를 아직 쓰고 있어도 컴파일 오류가 안 나도록 유지.
     */
    public void getQuizQuestionFromFirestore(
            int subjectId,
            int questionId,
            String difficultyLevel,
            OnQuestionFetchedListener listener
    ) {
        getQuizQuestionsFromFirestore(subjectId, difficultyLevel, 10, new OnQuestionsFetchedListener() {
            @Override
            public void onSuccess(List<QuizQuestion> questions) {
                if (questions == null || questions.isEmpty()) {
                    listener.onFailure(new Exception("출제 가능한 문제가 없습니다."));
                    return;
                }

                int index = questionId - 1;

                if (index >= 0 && index < questions.size()) {
                    listener.onSuccess(questions.get(index));
                } else {
                    listener.onFailure(new Exception("문제 번호(" + questionId + ")를 찾을 수 없습니다."));
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void tryStringSubjectId(
            int subjectId,
            String difficultyLevel,
            int maxQuestionCount,
            OnQuestionsFetchedListener listener
    ) {
        db.collection(ROOT_COLLECTION)
                .whereEqualTo("subject_id", String.valueOf(subjectId))
                .get()
                .addOnSuccessListener(subjectDocs -> {
                    if (!subjectDocs.isEmpty()) {
                        DocumentSnapshot subjectDoc = subjectDocs.getDocuments().get(0);
                        fetchQuizList(subjectDoc, subjectId, difficultyLevel, maxQuestionCount, listener);
                    } else {
                        listener.onFailure(new Exception("DB에 과목 번호(" + subjectId + ")가 없습니다."));
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(
                        new Exception("과목 데이터 읽기 실패: " + e.getMessage())
                ));
    }

    private void fetchQuizList(
            DocumentSnapshot subjectDoc,
            int subjectId,
            String selectedDifficultyLevel,
            int maxQuestionCount,
            OnQuestionsFetchedListener listener
    ) {
        subjectDoc.getReference()
                .collection(SUB_COLLECTION)
                .get()
                .addOnSuccessListener(quizDocs -> {
                    List<QuizQuestion> allQuestions = new ArrayList<>();
                    List<QuizQuestion> difficultyMatchedQuestions = new ArrayList<>();

                    for (DocumentSnapshot doc : quizDocs.getDocuments()) {
                        QuizQuestion question = parseQuestion(doc, subjectId, selectedDifficultyLevel);

                        if (question == null) {
                            continue;
                        }

                        allQuestions.add(question);

                        if (selectedDifficultyLevel.equals(question.getDifficultyLevel())) {
                            difficultyMatchedQuestions.add(question);
                        }
                    }

                    List<QuizQuestion> result;

                    /*
                     * Firebase에 difficulty_level이 제대로 들어가 있으면 해당 난이도만 출제.
                     * 아직 difficulty_level이 없거나 값이 불일치하면 전체 문제를 fallback으로 사용.
                     */
                    if (!difficultyMatchedQuestions.isEmpty()) {
                        result = difficultyMatchedQuestions;
                    } else {
                        result = allQuestions;
                    }

                    if (result.isEmpty()) {
                        listener.onFailure(new Exception("출제 가능한 문제가 없습니다."));
                        return;
                    }

                    Collections.shuffle(result);

                    if (result.size() > maxQuestionCount) {
                        result = new ArrayList<>(result.subList(0, maxQuestionCount));
                    }

                    listener.onSuccess(result);
                })
                .addOnFailureListener(e -> listener.onFailure(
                        new Exception("문제 데이터 읽기 실패: " + e.getMessage())
                ));
    }

    @SuppressWarnings("unchecked")
    private QuizQuestion parseQuestion(
            DocumentSnapshot doc,
            int subjectId,
            String selectedDifficultyLevel
    ) {
        String questionText = doc.getString("question");

        Object optionsObj = doc.get("answer_choice");
        List<String> optionsList = null;

        if (optionsObj instanceof List) {
            optionsList = (List<String>) optionsObj;
        }

        int answerIndex = 0;
        Object correctObj = doc.get("answer_correct");

        if (correctObj instanceof Number) {
            answerIndex = ((Number) correctObj).intValue();
        } else if (correctObj instanceof String) {
            try {
                answerIndex = Integer.parseInt((String) correctObj);
            } catch (Exception ignored) {
                answerIndex = 0;
            }
        }

        if (questionText == null || optionsList == null || optionsList.isEmpty()) {
            return null;
        }

        int quizId = subjectId;
        Object quizIdObj = doc.get("quiz_id");

        if (quizIdObj instanceof Number) {
            quizId = ((Number) quizIdObj).intValue();
        } else if (quizIdObj instanceof String) {
            try {
                quizId = Integer.parseInt((String) quizIdObj);
            } catch (Exception ignored) {
                quizId = subjectId;
            }
        }

        String difficulty = doc.getString("difficulty_level");

        /*
         * Firebase에 difficulty_level이 없으면
         * 사용자가 선택한 난이도를 현재 문제 난이도로 간주한다.
         *
         * 이렇게 해야 상 문제를 선택했는데도
         * getDifficultyLevel()이 easy로 들어가 점수가 10점으로 계산되는 문제를 막을 수 있다.
         */
        if (difficulty == null || difficulty.trim().isEmpty()) {
            difficulty = selectedDifficultyLevel;
        }

        String[] options = optionsList.toArray(new String[0]);

        return new QuizQuestion(
                quizId,
                questionText,
                options,
                answerIndex,
                difficulty
        );
    }
}