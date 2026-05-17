package com.example.term_project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KnapsackQuizSelector {

    private static class State {
        int score;
        int easyCount;
        int normalCount;
        int hardCount;
        List<QuizQuestion> questions;

        State() {
            this.score = 0;
            this.easyCount = 0;
            this.normalCount = 0;
            this.hardCount = 0;
            this.questions = new ArrayList<>();
        }

        State(State other) {
            this.score = other.score;
            this.easyCount = other.easyCount;
            this.normalCount = other.normalCount;
            this.hardCount = other.hardCount;
            this.questions = new ArrayList<>(other.questions);
        }

        int totalCount() {
            return questions.size();
        }
    }

    public static List<QuizQuestion> selectQuestions(
            List<QuizQuestion> allQuestions,
            int targetScore,
            int maxEasy,
            int maxNormal,
            int maxHard,
            int maxTotal
    ) {
        if (allQuestions == null || allQuestions.isEmpty()) {
            return new ArrayList<>();
        }

        // 매번 같은 조합만 나오는 것을 줄이기 위해 섞음
        List<QuizQuestion> candidates = new ArrayList<>(allQuestions);
        Collections.shuffle(candidates);

        State[] dp = new State[targetScore + 1];
        dp[0] = new State();

        for (QuizQuestion question : candidates) {
            int qScore = getScoreByDifficulty(question.getDifficultyLevel());

            if (qScore <= 0 || qScore > targetScore) {
                continue;
            }

            /*
             * 0-1 Knapsack 핵심
             * 뒤에서 앞으로 순회해야 같은 문제를 중복 선택하지 않음
             */
            for (int score = targetScore; score >= qScore; score--) {
                if (dp[score - qScore] == null) {
                    continue;
                }

                State prev = dp[score - qScore];
                State next = new State(prev);

                if (!canAddQuestion(next, question, maxEasy, maxNormal, maxHard, maxTotal)) {
                    continue;
                }

                next.questions.add(question);
                next.score += qScore;
                increaseDifficultyCount(next, question.getDifficultyLevel());

                if (dp[score] == null || isBetter(next, dp[score])) {
                    dp[score] = next;
                }
            }
        }

        /*
         * targetScore를 넘지 않는 선에서 가장 가까운 점수 선택
         */
        for (int score = targetScore; score >= 0; score--) {
            if (dp[score] != null && !dp[score].questions.isEmpty()) {
                return dp[score].questions;
            }
        }

        return new ArrayList<>();
    }

    private static int getScoreByDifficulty(String difficulty) {
        if ("easy".equals(difficulty)) {
            return 10;
        }

        if ("normal".equals(difficulty)) {
            return 20;
        }

        if ("hard".equals(difficulty)) {
            return 30;
        }

        return 0;
    }

    private static boolean canAddQuestion(
            State state,
            QuizQuestion question,
            int maxEasy,
            int maxNormal,
            int maxHard,
            int maxTotal
    ) {
        if (state.totalCount() >= maxTotal) {
            return false;
        }

        String difficulty = question.getDifficultyLevel();

        if ("easy".equals(difficulty)) {
            return state.easyCount < maxEasy;
        }

        if ("normal".equals(difficulty)) {
            return state.normalCount < maxNormal;
        }

        if ("hard".equals(difficulty)) {
            return state.hardCount < maxHard;
        }

        return false;
    }

    private static void increaseDifficultyCount(State state, String difficulty) {
        if ("easy".equals(difficulty)) {
            state.easyCount++;
        } else if ("normal".equals(difficulty)) {
            state.normalCount++;
        } else if ("hard".equals(difficulty)) {
            state.hardCount++;
        }
    }

    private static boolean isBetter(State next, State current) {
        /*
         * 같은 점수 조합이 여러 개라면
         * 난이도가 더 다양하게 섞인 조합을 우선 선택
         */
        int nextVariety = getDifficultyVariety(next);
        int currentVariety = getDifficultyVariety(current);

        if (nextVariety != currentVariety) {
            return nextVariety > currentVariety;
        }

        /*
         * 다양성이 같다면 문제 수가 적은 조합 선택
         * 쉬운 문제 여러 개로만 채워지는 것을 한 번 더 방지
         */
        return next.totalCount() < current.totalCount();
    }

    private static int getDifficultyVariety(State state) {
        int variety = 0;

        if (state.easyCount > 0) {
            variety++;
        }

        if (state.normalCount > 0) {
            variety++;
        }

        if (state.hardCount > 0) {
            variety++;
        }

        return variety;
    }
}