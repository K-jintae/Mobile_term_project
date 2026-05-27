package com.example.term_project;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class BattleRequestDialogFragment extends DialogFragment {

    public interface OnBattleRequestConfirmListener {
        void onBattleRequestConfirm(FriendItem targetFriend, int betGold, int subjectNo, String difficulty);
    }

    private static final String ARG_TARGET_UID = "targetUid";
    private static final String ARG_TARGET_NAME = "targetName";
    private static final String ARG_TARGET_LEVEL = "targetLevel";

    private FriendItem targetFriend;

    private final int defaultBetGold = 100;
    private final int defaultSubjectNo = 1;
    private final String defaultDifficulty = "상";

    public static BattleRequestDialogFragment newInstance(FriendItem friendItem) {
        BattleRequestDialogFragment fragment = new BattleRequestDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TARGET_UID, friendItem.getUid());
        args.putString(ARG_TARGET_NAME, friendItem.getName());
        args.putString(ARG_TARGET_LEVEL, friendItem.getLevel());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog_Alert);

        String uid = "";
        String name = "상대";
        String level = "없음";

        if (getArguments() != null) {
            uid = getArguments().getString(ARG_TARGET_UID, "");
            name = getArguments().getString(ARG_TARGET_NAME, "상대");
            level = getArguments().getString(ARG_TARGET_LEVEL, "없음");
        }

        targetFriend = new FriendItem(uid, name, "confirmed", level);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.dialog_battle_request, container, false);

        TextView tvTitle = view.findViewById(R.id.tvBattleDialogTitle);
        TextView tvGold = view.findViewById(R.id.tvBattleGold);
        TextView tvSubject = view.findViewById(R.id.tvBattleSubject);
        TextView tvDifficulty = view.findViewById(R.id.tvBattleDifficulty);
        TextView btnCancel = view.findViewById(R.id.btnBattleCancel);
        TextView btnApply = view.findViewById(R.id.btnBattleApply);

        tvTitle.setText(targetFriend.getName() + "님에게 대전 신청");
        tvGold.setText("베팅 골드 기본값: " + defaultBetGold);
        tvSubject.setText("과목 번호 기본값: " + defaultSubjectNo);
        tvDifficulty.setText("난이도 기본값: " + defaultDifficulty);

        btnCancel.setOnClickListener(v -> dismiss());

        btnApply.setOnClickListener(v -> {
            if (getActivity() instanceof OnBattleRequestConfirmListener) {
                ((OnBattleRequestConfirmListener) getActivity())
                        .onBattleRequestConfirm(
                                targetFriend,
                                defaultBetGold,
                                defaultSubjectNo,
                                defaultDifficulty
                        );
            }

            dismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();

        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.82);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}