package com.example.term_project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizSelector {

    public static class QuizRule {
        public int targetScore;
        public int maxEasy;
        public int maxNormal;
        public int maxHard;

        public QuizRule(int targetScore, int maxEasy, int maxNormal, int maxHard) {
            this.targetScore = targetScore;
            this.maxEasy = maxEasy;
            this.maxNormal = maxNormal;
            this.maxHard = maxHard;
        }
    }

    public static QuizRule getRuleByDifficulty(String selectedDifficulty) {
        if ("hard".equals(selectedDifficulty)) {
            // 상 단계: 쉬움 2개, 중간 3개, 어려움 3개까지
            return new QuizRule(180, 2, 3, 3);
        }

        if ("normal".equals(selectedDifficulty)) {
            // 중 단계: 쉬움 3개, 중간 3개, 어려움 1개까지
            return new QuizRule(120, 3, 3, 1);
        }

        // 하 단계: 쉬움 5개, 중간 1개, 어려움 0개까지
        return new QuizRule(75, 5, 1, 0);
    }

    public static int getScoreByDifficulty(String difficultyLevel) {
        if ("hard".equals(difficultyLevel)) {
            return 45;
        }

        if ("normal".equals(difficultyLevel)) {
            return 30;
        }

        return 15;
    }

    public static List<QuizQuestion> selectQuestions(
            List<QuizQuestion> allQuestions,
            String selectedDifficulty
    ) {
        QuizRule rule = getRuleByDifficulty(selectedDifficulty);

        // 매번 같은 문제가 나오지 않도록 먼저 섞음
        List<QuizQuestion> shuffled = new ArrayList<>(allQuestions);
        Collections.shuffle(shuffled);

        Result best = new Result();

        backtrack(
                shuffled,
                0,
                rule,
                new ArrayList<>(),
                0,
                0,
                0,
                0,
                best
        );

        return best.questions;
    }

    private static void backtrack(
            List<QuizQuestion> questions,
            int index,
            QuizRule rule,
            List<QuizQuestion> selected,
            int currentScore,
            int easyCount,
            int normalCount,
            int hardCount,
            Result best
    ) {
        if (currentScore > rule.targetScore) {
            return;
        }

        if (isBetter(currentScore, selected, best)) {
            best.score = currentScore;
            best.questions = new ArrayList<>(selected);
        }

        if (index >= questions.size()) {
            return;
        }

        QuizQuestion current = questions.get(index);

        // 1. 현재 문제를 선택하지 않는 경우
        backtrack(
                questions,
                index + 1,
                rule,
                selected,
                currentScore,
                easyCount,
                normalCount,
                hardCount,
                best
        );

        String level = current.getDifficultyLevel();
        int score = getScoreByDifficulty(level);

        int nextEasy = easyCount;
        int nextNormal = normalCount;
        int nextHard = hardCount;

        if ("hard".equals(level)) {
            nextHard++;
            if (nextHard > rule.maxHard) {
                return;
            }
        } else if ("normal".equals(level)) {
            nextNormal++;
            if (nextNormal > rule.maxNormal) {
                return;
            }
        } else {
            nextEasy++;
            if (nextEasy > rule.maxEasy) {
                return;
            }
        }

        if (currentScore + score > rule.targetScore) {
            return;
        }

        // 2. 현재 문제를 선택하는 경우
        selected.add(current);

        backtrack(
                questions,
                index + 1,
                rule,
                selected,
                currentScore + score,
                nextEasy,
                nextNormal,
                nextHard,
                best
        );

        selected.remove(selected.size() - 1);
    }

    private static boolean isBetter(
            int currentScore,
            List<QuizQuestion> selected,
            Result best
    ) {
        if (selected.isEmpty()) {
            return false;
        }

        if (currentScore > best.score) {
            return true;
        }

        // 점수가 같으면 문제 수가 더 적은 조합을 선택
        // 너무 많은 쉬운 문제로 채워지는 것을 추가로 방지
        if (currentScore == best.score && selected.size() < best.questions.size()) {
            return true;
        }

        return false;
    }

    private static class Result {
        int score = 0;
        List<QuizQuestion> questions = new ArrayList<>();
    }
}