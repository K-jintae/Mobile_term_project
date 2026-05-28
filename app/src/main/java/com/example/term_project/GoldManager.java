package com.example.term_project;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class GoldManager {

    public interface GoldCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public static void lockBetGold(String playerA, String playerB, int betGold, GoldCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference aRef = db.collection("users").document(playerA);
        DocumentReference bRef = db.collection("users").document(playerB);

        db.runTransaction(transaction -> {
            Long aGoldLong = transaction.get(aRef).getLong("gold");
            Long bGoldLong = transaction.get(bRef).getLong("gold");

            long aGold = aGoldLong != null ? aGoldLong : 0L;
            long bGold = bGoldLong != null ? bGoldLong : 0L;

            if (aGold < betGold || bGold < betGold) {
                throw new FirebaseFirestoreException(
                        "둘 중 한 명의 골드가 부족합니다.",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            transaction.update(aRef, "gold", aGold - betGold);
            transaction.update(bRef, "gold", bGold - betGold);

            return null;
        }).addOnSuccessListener(unused -> {
            if (callback != null) {
                callback.onSuccess();
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onFailure("골드 차감 실패: " + e.getMessage());
            }
        });
    }

    public static void settleBattleGold(
            String playerA,
            String playerB,
            int betGold,
            int scoreA,
            int scoreB,
            GoldCallback callback
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference aRef = db.collection("users").document(playerA);
        DocumentReference bRef = db.collection("users").document(playerB);

        db.runTransaction(transaction -> {
            Long aGoldLong = transaction.get(aRef).getLong("gold");
            Long bGoldLong = transaction.get(bRef).getLong("gold");

            long aGold = aGoldLong != null ? aGoldLong : 0L;
            long bGold = bGoldLong != null ? bGoldLong : 0L;

            int pot = betGold * 2;

            if (scoreA > scoreB) {
                transaction.update(aRef, "gold", aGold + pot);
            } else if (scoreB > scoreA) {
                transaction.update(bRef, "gold", bGold + pot);
            } else {
                transaction.update(aRef, "gold", aGold + betGold);
                transaction.update(bRef, "gold", bGold + betGold);
            }

            return null;
        }).addOnSuccessListener(unused -> {
            if (callback != null) {
                callback.onSuccess();
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onFailure("골드 정산 실패: " + e.getMessage());
            }
        });
    }

    public static void settleBattleGoldByWinner(
            String winnerUid,
            String loserUid,
            int betGold,
            GoldCallback callback
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference winnerRef = db.collection("users").document(winnerUid);
        DocumentReference loserRef = db.collection("users").document(loserUid);

        db.runTransaction(transaction -> {
            Long winnerGoldLong = transaction.get(winnerRef).getLong("gold");
            Long loserGoldLong = transaction.get(loserRef).getLong("gold");

            long winnerGold = winnerGoldLong != null ? winnerGoldLong : 0L;
            long loserGold = loserGoldLong != null ? loserGoldLong : 0L;

            int pot = betGold * 2;

            transaction.update(winnerRef, "gold", winnerGold + pot);
            transaction.update(loserRef, "gold", loserGold);

            return null;
        }).addOnSuccessListener(unused -> {
            if (callback != null) {
                callback.onSuccess();
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onFailure("몰수승 골드 정산 실패: " + e.getMessage());
            }
        });
    }
}