package com.example.term_project;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class QuizRepository {

    private FirebaseFirestore db;

    // db 구조 파악
    private final String ROOT_COLLECTION = "subjects";
    private final String SUB_COLLECTION = "quizzes";

    public QuizRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnQuestionFetchedListener {
        void onSuccess(QuizQuestion question);
        void onFailure(Exception e);
    }

    // 과목 번호와 문제 번호로 문제 찾기
    public void getQuizQuestionFromFirestore(int subjectId, int questionId, OnQuestionFetchedListener listener) {
        db.collection(ROOT_COLLECTION)
                .whereEqualTo("subject_id", subjectId)
                .get()
                .addOnSuccessListener(subjectDocs -> {
                    if (!subjectDocs.isEmpty()) {
                        DocumentSnapshot subjectDoc = subjectDocs.getDocuments().get(0);
                        subjectDoc.getReference().collection(SUB_COLLECTION)
                                .whereEqualTo("quiz_id", questionId)
                                .get()
                                .addOnSuccessListener(quizDocs -> {
                                    if (!quizDocs.isEmpty()) {
                                        parseAndSend(quizDocs.getDocuments().get(0), subjectId, listener);
                                    } else {
                                        tryStringQuizId(subjectDoc, subjectId, questionId, listener);
                                    }
                                })
                                .addOnFailureListener(e -> listener.onFailure(new Exception("문제 상자 읽기 실패: " + e.getMessage())));
                    } else {
                        tryStringSubjectId(subjectId, questionId, listener);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(new Exception("과목 상자 읽기 실패: " + e.getMessage())));
    }

    private void tryStringQuizId(DocumentSnapshot subjectDoc, int subjectId, int questionId, OnQuestionFetchedListener listener) {
        subjectDoc.getReference().collection(SUB_COLLECTION)
                .whereEqualTo("quiz_id", String.valueOf(questionId))
                .get()
                .addOnSuccessListener(quizDocs -> {
                    if (!quizDocs.isEmpty()) {
                        parseAndSend(quizDocs.getDocuments().get(0), subjectId, listener);
                    } else {
                        listener.onFailure(new Exception("문제 번호(" + questionId + ")를 찾을 수 없습니다."));
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(new Exception("문제 상자 읽기 실패: " + e.getMessage())));
    }

    private void tryStringSubjectId(int subjectId, int questionId, OnQuestionFetchedListener listener) {
        db.collection(ROOT_COLLECTION)
                .whereEqualTo("subject_id", String.valueOf(subjectId))
                .get()
                .addOnSuccessListener(subjectDocs -> {
                    if (!subjectDocs.isEmpty()) {
                        DocumentSnapshot subjectDoc = subjectDocs.getDocuments().get(0);
                        subjectDoc.getReference().collection(SUB_COLLECTION)
                                .whereEqualTo("quiz_id", questionId)
                                .get()
                                .addOnSuccessListener(quizDocs -> {
                                    if (!quizDocs.isEmpty()) {
                                        parseAndSend(quizDocs.getDocuments().get(0), subjectId, listener);
                                    } else {
                                        tryStringQuizId(subjectDoc, subjectId, questionId, listener);
                                    }
                                });
                    } else {
                        listener.onFailure(new Exception("DB에 과목 번호(" + subjectId + ")가 없습니다."));
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(new Exception("과목 상자 읽기 실패: " + e.getMessage())));
    }

    // 오류 방지용 코드
    @SuppressWarnings("unchecked")
    private void parseAndSend(DocumentSnapshot doc, int subjectId, OnQuestionFetchedListener listener) {
        String questionText = doc.getString("question");

        List<String> optionsList = null;
        Object optionsObj = doc.get("answer_choice");
        if (optionsObj instanceof List) {
            optionsList = (List<String>) optionsObj;
        }

        int answerIndex = 0;
        Object correctObj = doc.get("answer_correct");
        if (correctObj instanceof Number) {
            answerIndex = ((Number) correctObj).intValue();
        } else if (correctObj instanceof String) {
            try { answerIndex = Integer.parseInt((String) correctObj); } catch (Exception ignored) {}
        }

        if (questionText != null && optionsList != null) {
            String[] options = optionsList.toArray(new String[0]);
            String difficulty = doc.getString("difficulty_level");
            String diff = (difficulty != null) ? difficulty : "easy";

            QuizQuestion question = new QuizQuestion(subjectId, questionText, options, answerIndex, diff);
            listener.onSuccess(question);
        } else {
            listener.onFailure(new Exception("question 또는 answer_choice 데이터가 비어있습니다."));
        }
    }
}