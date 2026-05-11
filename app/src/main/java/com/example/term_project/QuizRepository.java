package com.example.term_project;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class QuizRepository {

    private FirebaseFirestore db;

    // Firestore 구조
    private final String ROOT_COLLECTION = "subjects";
    private final String SUB_COLLECTION = "quizzes";

    public QuizRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnQuestionFetchedListener {
        void onSuccess(QuizQuestion question);
        void onFailure(Exception e);
    }

    /*
     * 현재 사용 버전
     *
     * 목적:
     * - 원본 레포지토리 방식과 동일하게 문제를 가져온다.
     * - subject_id + quiz_id 조건만 사용한다.
     * - difficulty_level은 검색 조건으로 사용하지 않는다.
     *
     * 이유:
     * - Firebase에 조원이 넣어둔 기존 문제 구조가 아직 확실하지 않음.
     * - difficulty_level 값이 없거나 normal/easy/hard와 다르면 문제가 안 뜰 수 있음.
     * - 따라서 우선 원본 방식으로 문제를 정상 출력시키는 것이 우선.
     *
     * QuizPlayFragment에서는 difficultyLevel을 넘겨주지만,
     * 여기서는 아직 검색 조건에 사용하지 않는다.
     */
    public void getQuizQuestionFromFirestore(
            int subjectId,
            int questionId,
            String difficultyLevel,
            OnQuestionFetchedListener listener
    ) {
        db.collection(ROOT_COLLECTION)
                .whereEqualTo("subject_id", subjectId)
                .get()
                .addOnSuccessListener(subjectDocs -> {
                    if (!subjectDocs.isEmpty()) {
                        DocumentSnapshot subjectDoc = subjectDocs.getDocuments().get(0);

                        // 원본 방식: quiz_id만으로 문제 검색
                        subjectDoc.getReference()
                                .collection(SUB_COLLECTION)
                                .whereEqualTo("quiz_id", questionId)
                                .get()
                                .addOnSuccessListener(quizDocs -> {
                                    if (!quizDocs.isEmpty()) {
                                        parseAndSend(quizDocs.getDocuments().get(0), subjectId, listener);
                                    } else {
                                        tryStringQuizId(subjectDoc, subjectId, questionId, listener);
                                    }
                                })
                                .addOnFailureListener(e ->
                                        listener.onFailure(
                                                new Exception("문제 데이터 읽기 실패: " + e.getMessage())
                                        )
                                );
                    } else {
                        tryStringSubjectId(subjectId, questionId, listener);
                    }
                })
                .addOnFailureListener(e ->
                        listener.onFailure(
                                new Exception("과목 데이터 읽기 실패: " + e.getMessage())
                        )
                );
    }

    /*
     * subject_id가 문자열 "1" 형태로 저장되어 있을 경우 대비
     */
    private void tryStringSubjectId(
            int subjectId,
            int questionId,
            OnQuestionFetchedListener listener
    ) {
        db.collection(ROOT_COLLECTION)
                .whereEqualTo("subject_id", String.valueOf(subjectId))
                .get()
                .addOnSuccessListener(subjectDocs -> {
                    if (!subjectDocs.isEmpty()) {
                        DocumentSnapshot subjectDoc = subjectDocs.getDocuments().get(0);

                        subjectDoc.getReference()
                                .collection(SUB_COLLECTION)
                                .whereEqualTo("quiz_id", questionId)
                                .get()
                                .addOnSuccessListener(quizDocs -> {
                                    if (!quizDocs.isEmpty()) {
                                        parseAndSend(quizDocs.getDocuments().get(0), subjectId, listener);
                                    } else {
                                        tryStringQuizId(subjectDoc, subjectId, questionId, listener);
                                    }
                                })
                                .addOnFailureListener(e ->
                                        listener.onFailure(
                                                new Exception("문제 데이터 읽기 실패: " + e.getMessage())
                                        )
                                );
                    } else {
                        listener.onFailure(
                                new Exception("DB에 과목 번호(" + subjectId + ")가 없습니다.")
                        );
                    }
                })
                .addOnFailureListener(e ->
                        listener.onFailure(
                                new Exception("과목 데이터 읽기 실패: " + e.getMessage())
                        )
                );
    }

    /*
     * quiz_id가 문자열 "1" 형태로 저장되어 있을 경우 대비
     */
    private void tryStringQuizId(
            DocumentSnapshot subjectDoc,
            int subjectId,
            int questionId,
            OnQuestionFetchedListener listener
    ) {
        subjectDoc.getReference()
                .collection(SUB_COLLECTION)
                .whereEqualTo("quiz_id", String.valueOf(questionId))
                .get()
                .addOnSuccessListener(quizDocs -> {
                    if (!quizDocs.isEmpty()) {
                        parseAndSend(quizDocs.getDocuments().get(0), subjectId, listener);
                    } else {
                        listener.onFailure(
                                new Exception("문제 번호(" + questionId + ")를 찾을 수 없습니다.")
                        );
                    }
                })
                .addOnFailureListener(e ->
                        listener.onFailure(
                                new Exception("문제 데이터 읽기 실패: " + e.getMessage())
                        )
                );
    }

    /*
     * Firestore 문서를 QuizQuestion 객체로 변환
     */
    @SuppressWarnings("unchecked")
    private void parseAndSend(
            DocumentSnapshot doc,
            int subjectId,
            OnQuestionFetchedListener listener
    ) {
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
            try {
                answerIndex = Integer.parseInt((String) correctObj);
            } catch (Exception ignored) {
                answerIndex = 0;
            }
        }

        if (questionText != null && optionsList != null && !optionsList.isEmpty()) {
            String[] options = optionsList.toArray(new String[0]);

            /*
             * difficulty_level은 읽기만 한다.
             * 현재는 검색 조건으로 쓰지 않는다.
             *
             * Firestore에 difficulty_level이 없으면 기본값 easy로 처리한다.
             */
            String difficulty = doc.getString("difficulty_level");
            String diff = difficulty != null ? difficulty : "easy";

            /*
             * 주의:
             * 원본 코드에서는 첫 번째 인자로 subjectId를 넣고 있었음.
             * 현재 QuizQuestion 생성자가
             * QuizQuestion(int quizId, String question, String[] options, int correctAnswerIndex, String difficultyLevel)
             * 형태라면 subjectId 대신 quizId를 넣는 게 더 정확함.
             *
             * 다만 기존 원본 호환을 우선하려면 subjectId를 넣어도 앱 동작에는 큰 영향이 없을 수 있음.
             */
            QuizQuestion question = new QuizQuestion(
                    subjectId,
                    questionText,
                    options,
                    answerIndex,
                    diff
            );

            listener.onSuccess(question);
        } else {
            listener.onFailure(
                    new Exception("question 또는 answer_choice 데이터가 비어있습니다.")
            );
        }
    }

    /*
     =====================================================================
     나중에 Firebase 구조 확인 후 교체할 난이도 필터 버전
     =====================================================================

     아래 코드는 아직 사용하지 마세요.

     사용 조건:
     1. 각 문제 문서에 difficulty_level 필드가 있어야 함.
     2. 값은 반드시 아래 중 하나여야 함.
        - easy
        - normal
        - hard
     3. 각 난이도별 quiz_id가 1부터 순서대로 있어야 함.

     예시:
     subjects
      └─ 과목문서
          ├─ subject_id: 1
          └─ quizzes
              ├─ 문제문서
              │   ├─ quiz_id: 1
              │   ├─ difficulty_level: "normal"
              │   ├─ question: "문제 내용"
              │   ├─ answer_choice: ["보기1", "보기2", "보기3", "보기4"]
              │   └─ answer_correct: 0

     나중에 이 버전으로 바꾸려면:
     - 위의 현재 getQuizQuestionFromFirestore() 메서드 내부 검색 부분에서
       .whereEqualTo("quiz_id", questionId)
       아래에
       .whereEqualTo("difficulty_level", difficultyLevel)
       을 추가하면 됨.

     정확한 교체 코드는 아래 참고.
     */

    /*
    public void getQuizQuestionFromFirestore(
            int subjectId,
            int questionId,
            String difficultyLevel,
            OnQuestionFetchedListener listener
    ) {
        db.collection(ROOT_COLLECTION)
                .whereEqualTo("subject_id", subjectId)
                .get()
                .addOnSuccessListener(subjectDocs -> {
                    if (!subjectDocs.isEmpty()) {
                        DocumentSnapshot subjectDoc = subjectDocs.getDocuments().get(0);

                        subjectDoc.getReference()
                                .collection(SUB_COLLECTION)
                                .whereEqualTo("quiz_id", questionId)
                                .whereEqualTo("difficulty_level", difficultyLevel)
                                .get()
                                .addOnSuccessListener(quizDocs -> {
                                    if (!quizDocs.isEmpty()) {
                                        parseAndSend(quizDocs.getDocuments().get(0), subjectId, listener);
                                    } else {
                                        listener.onFailure(
                                                new Exception(
                                                        "과목 " + subjectId
                                                                + "의 " + difficultyLevel
                                                                + " 난이도 문제 번호("
                                                                + questionId
                                                                + ")를 찾을 수 없습니다."
                                                )
                                        );
                                    }
                                })
                                .addOnFailureListener(e ->
                                        listener.onFailure(
                                                new Exception("문제 데이터 읽기 실패: " + e.getMessage())
                                        )
                                );
                    } else {
                        listener.onFailure(
                                new Exception("DB에 과목 번호(" + subjectId + ")가 없습니다.")
                        );
                    }
                })
                .addOnFailureListener(e ->
                        listener.onFailure(
                                new Exception("과목 데이터 읽기 실패: " + e.getMessage())
                        )
                );
    }
    */
}