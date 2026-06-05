package com.example.term_project;

import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RightFragment extends Fragment {

    private GridLayout imageGrid;
    private CharacterViewModel viewModel;

    private ImageView bgPreview;
    private ImageView faceImage;
    private ImageView hatImage;
    private ImageView clothesImage;

    private AppCompatButton btnAll, btnHat, btnTop, btnInterior, btnReset;
    private FrameLayout tabAllBox, tabHatBox, tabTopBox, tabInteriorBox;

    // 확인 팝업용 뷰
    private View confirmDimView;
    private LinearLayout changeConfirmPopup;
    private AppCompatButton btnConfirmYes, btnConfirmNo;

    // 첫 번째 코드의 펜딩 상태 관리 변수
    private boolean pendingIsRemove = false;
    private String pendingRemoveType = "";
    private int pendingResId = 0;
    private boolean pendingIsInterior = false;

    // 구매 처리를 위한 펜딩 변수
    private boolean pendingIsPurchase = false;
    private String pendingItemName = "";

    // Firebase 및 상점 데이터 리스트
    private List<String> unlockedItems = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid;

    private static final int TAB_ALL = 0;
    private static final int TAB_HAT = 1;
    private static final int TAB_TOP = 2;
    private static final int TAB_INTERIOR = 3;

    private int currentTab = TAB_ALL;
    private static final int HAT_PRICE = 100;
    private static final int CLOTHES_PRICE = 200;
    private static final int INTERIOR_PRICE = 300; // 모든 아이템 가격 10골드 고정

    private int getItemPrice(String itemName, boolean isInterior) {
        if (isInterior) return INTERIOR_PRICE;

        if (itemName.startsWith("hat_")) return HAT_PRICE;

        if (itemName.startsWith("clothes_")) return CLOTHES_PRICE;

        return HAT_PRICE;
    }
    private final List<DressItem> hatList = Arrays.asList(
            new DressItem(R.drawable.thumb_hat_halloween, R.drawable.hat_halloween),
            new DressItem(R.drawable.thumb_hat_hiphop, R.drawable.hat_hiphop),
            new DressItem(R.drawable.thumb_hat_onepiece, R.drawable.hat_onepiece),
            new DressItem(R.drawable.thumb_hat_crown, R.drawable.hat_crown),
            new DressItem(R.drawable.thumb_hat_rabbit, R.drawable.hat_rabbit),
            new DressItem(R.drawable.thumb_hat_pokemon, R.drawable.hat_pokemon),
            new DressItem(R.drawable.thumb_hat_santa, R.drawable.hat_santa),
            new DressItem(R.drawable.thumb_hat_gojo, R.drawable.hat_gojo),
            new DressItem(R.drawable.thumb_hat_sunglass, R.drawable.hat_sunglass),
            new DressItem(R.drawable.thumb_hat_snowman, R.drawable.hat_snowman),
            new DressItem(R.drawable.thumb_hat_astronaut, R.drawable.hat_astronaut)
    );

    private final List<DressItem> clothesList = Arrays.asList(
            new DressItem(R.drawable.thumb_clothes_halloween, R.drawable.clothes_halloween),
            new DressItem(R.drawable.thumb_clothes_hiphop, R.drawable.clothes_hiphop),
            new DressItem(R.drawable.thumb_clothes_onepiece, R.drawable.clothes_onepiece),
            new DressItem(R.drawable.thumb_clothes_pokemon, R.drawable.clothes_pokemon),
            new DressItem(R.drawable.thumb_clothes_santa, R.drawable.clothes_santa),
            new DressItem(R.drawable.thumb_clothes_hoodie, R.drawable.clothes_hoodie),
            new DressItem(R.drawable.thumb_clothes_poor, R.drawable.clothes_poor),
            new DressItem(R.drawable.thumb_clothes_rabbit, R.drawable.clothes_rabbit),
            new DressItem(R.drawable.thumb_clothes_snowman, R.drawable.clothes_snowman),
            new DressItem(R.drawable.thumb_clothes_gojo, R.drawable.clothes_gojo),
            new DressItem(R.drawable.thumb_clothes_brucelee, R.drawable.clothes_brucelee),
            new DressItem(R.drawable.thumb_clothes_astronaut, R.drawable.clothes_astronaut)
    );

    private final List<Integer> interiorList = Arrays.asList(
            R.drawable.background_hill,
            R.drawable.background_room,
            R.drawable.background_space
    );

    public RightFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_right, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CharacterViewModel.class);

        // Firebase 초기화
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        imageGrid = view.findViewById(R.id.image_grid);
        bgPreview = view.findViewById(R.id.bg_preview);
        faceImage = view.findViewById(R.id.face_image);
        hatImage = view.findViewById(R.id.hat_image);
        clothesImage = view.findViewById(R.id.clothes_image);

        FrameLayout topArea = view.findViewById(R.id.top_area);

        btnReset = view.findViewById(R.id.btn_reset);
        btnAll = view.findViewById(R.id.btn_all);
        btnHat = view.findViewById(R.id.btn_hat);
        btnTop = view.findViewById(R.id.btn_top);
        btnInterior = view.findViewById(R.id.btn_interior);

        tabAllBox = view.findViewById(R.id.tab_all_box);
        tabHatBox = view.findViewById(R.id.tab_hat_box);
        tabTopBox = view.findViewById(R.id.tab_top_box);
        tabInteriorBox = view.findViewById(R.id.tab_interior_box);

        confirmDimView = view.findViewById(R.id.confirmDimView);
        changeConfirmPopup = view.findViewById(R.id.changeConfirmPopup);
        btnConfirmYes = view.findViewById(R.id.btnConfirmYes);
        btnConfirmNo = view.findViewById(R.id.btnConfirmNo);

        applyPressAnimation(btnReset);
        applyPressAnimation(tabAllBox);
        applyPressAnimation(tabHatBox);
        applyPressAnimation(tabTopBox);
        applyPressAnimation(tabInteriorBox);

        // 드래그 앤 드롭 해금 검증 결합
        topArea.setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                ClipData.Item item = event.getClipData().getItemAt(0);
                int resId = Integer.parseInt(item.getText().toString());
                String itemName = getResources().getResourceEntryName(resId);

                if (isItemUnlocked(itemName)) {
                    updateCharacter(resId);
                } else {
                    Toast.makeText(requireContext(), "아직 구매하지 않은 아이템입니다.", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        btnReset.setOnClickListener(v -> {
            resetCharacter();
            refreshCurrentTab();
        });

        tabAllBox.setOnClickListener(v -> {
            currentTab = TAB_ALL;
            updateTabButtons();
            showAllItems();
        });

        tabHatBox.setOnClickListener(v -> {
            currentTab = TAB_HAT;
            updateTabButtons();
            showDressItems(hatList);
        });

        tabTopBox.setOnClickListener(v -> {
            currentTab = TAB_TOP;
            updateTabButtons();
            showDressItems(clothesList);
        });

        tabInteriorBox.setOnClickListener(v -> {
            currentTab = TAB_INTERIOR;
            updateTabButtons();
            showInteriorItems(interiorList);
        });

        confirmDimView.setOnClickListener(v -> hideChangeConfirmPopup(null));

        // 예(Yes) 클릭 분기문 오류 정돈 병합
        btnConfirmYes.setOnClickListener(v -> {
            hideChangeConfirmPopup(() -> {
                if (pendingIsPurchase) {
                    executePurchaseItem(pendingItemName, pendingResId, pendingIsInterior);
                } else if (pendingIsRemove) {
                    removeEquippedItem(pendingRemoveType);
                } else {
                    if (pendingIsInterior) {
                        viewModel.setInterior(pendingResId);
                        String itemName = getResources().getResourceEntryName(pendingResId);
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).updateEquippedItem("interior", itemName);
                        }
                    } else {
                        updateCharacter(pendingResId);
                    }
                }
                resetPendingStates();
            });
        });

        btnConfirmNo.setOnClickListener(v -> {
            hideChangeConfirmPopup(null);
            resetPendingStates();
        });

        // LiveData 옵저버 중복 구문 정돈
        viewModel.getFace().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) faceImage.setImageResource(resId);
            else faceImage.setImageDrawable(null);
        });

        viewModel.getHat().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) hatImage.setImageResource(resId);
            else hatImage.setImageDrawable(null);
            refreshCurrentTab();
        });

        viewModel.getClothes().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) clothesImage.setImageResource(resId);
            else clothesImage.setImageDrawable(null);
            refreshCurrentTab();
        });

        viewModel.getInterior().observe(getViewLifecycleOwner(), resId -> {
            bgPreview.setImageResource(resId);
            refreshCurrentTab();
        });

        updateTabButtons();
        showAllItems();

        loadUserDataFromFirebase();

        return view;
    }

    private void resetPendingStates() {
        pendingIsRemove = false;
        pendingRemoveType = "";
        pendingIsPurchase = false;
        pendingItemName = "";
        pendingResId = 0;
        pendingIsInterior = false;
    }

    // Firestore에서 해금 장부 실시간 수신 시스템 보존
    private void loadUserDataFromFirebase() {
        if (uid == null) {
            showAllItems();
            return;
        }

        db.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        showAllItems();
                        return;
                    }
                    List<String> list = (List<String>) snapshot.get("unlocked_items");
                    unlockedItems = (list != null) ? list : new ArrayList<>();

                    refreshCurrentTab();
                });
    }

    private boolean isItemUnlocked(String itemName) {
        if ("background_hill".equals(itemName)) return true; // 기본 배경 예외 프리패스
        return unlockedItems.contains(itemName);
    }

    // 상점 재화 소모 및 서버 인벤토리 저장 핵심 처리 로직
    private void executePurchaseItem(String itemName, int resId, boolean isInterior) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null || uid == null) return;

        int price = getItemPrice(itemName, isInterior);

        if (mainActivity.spendGold(price)) {
            db.collection("users").document(uid)
                    .update(
                            "gold", mainActivity.getGold(),
                            "unlocked_items", FieldValue.arrayUnion(itemName)
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "구매가 완료되었습니다!", Toast.LENGTH_SHORT).show();
                        showChangeConfirmPopup(resId, isInterior);
                    })
                    .addOnFailureListener(e -> {
                        mainActivity.addGold(price); // 실패 시 환불 처리
                        Toast.makeText(requireContext(), "서버 통신 실패로 구매가 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(requireContext(), "재화가 부족하여 구매할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAllItems() {
        if (imageGrid == null) return;
        imageGrid.removeAllViews();
        for (DressItem item : hatList) addDressTile(item);
        for (DressItem item : clothesList) addDressTile(item);
        for (int resId : interiorList) addInteriorTile(resId);
    }

    private void showDressItems(List<DressItem> itemList) {
        if (imageGrid == null) return;
        imageGrid.removeAllViews();
        for (DressItem item : itemList) addDressTile(item);
    }

    private void showInteriorItems(List<Integer> imageList) {
        if (imageGrid == null) return;
        imageGrid.removeAllViews();
        for (int resId : imageList) addInteriorTile(resId);
    }

    // 옷/모자 타일 상점 기능 연동 결합
    private void addDressTile(DressItem item) {
        String itemName = getResources().getResourceEntryName(item.getApplyResId());
        boolean isUnlocked = isItemUnlocked(itemName);
        boolean selected = isDressSelected(item.getApplyResId());

        FrameLayout tile = createTile(selected);

        ImageView imageView = new ImageView(requireContext());
        imageView.setImageResource(item.getPreviewResId());
        imageView.setTag(item.getApplyResId());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        if (!isUnlocked) {
            imageView.setAlpha(0.4f);
        }

        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(imageParams);
        tile.addView(imageView);

        if (!isUnlocked) {
            addPriceLabel(tile, getItemPrice(itemName, false));
        }


        // 자물쇠 아이콘 레이어 결합
        if (!isUnlocked) {
            ImageView lockView = new ImageView(requireContext());
            lockView.setImageResource(R.drawable.lock);
            int lockSize = dpToPx(20);
            FrameLayout.LayoutParams lockParams = new FrameLayout.LayoutParams(lockSize, lockSize);
            lockParams.setMargins(dpToPx(4), dpToPx(4), 0, 0);
            lockView.setLayoutParams(lockParams);
            tile.addView(lockView);
        }

        imageView.setOnClickListener(v -> {
            int resId = (int) v.getTag();
            animateTileSelect(tile);

            if (!isUnlocked) {
                showPurchaseConfirmPopup(itemName, resId, false);
            } else {
                if (isDressSelected(resId)) {
                    if (containsApplyResId(hatList, resId)) {
                        showRemoveConfirmPopup(resId, "hat");
                    } else if (containsApplyResId(clothesList, resId)) {
                        showRemoveConfirmPopup(resId, "clothes");
                    }
                } else {
                    showChangeConfirmPopup(resId, false);
                }
            }
        });

        imageView.setOnLongClickListener(v -> {
            if (!isUnlocked) return false; // 미해금 상태는 드래그 차단
            ClipData data = ClipData.newPlainText("resId", v.getTag().toString());
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, null, 0);
            return true;
        });

        imageGrid.addView(tile);
    }

    // 인테리어 배경 타일 상점 기능 연동 결합
    private void addInteriorTile(int resId) {
        String itemName = getResources().getResourceEntryName(resId);
        boolean isUnlocked = isItemUnlocked(itemName);
        boolean selected = isInteriorSelected(resId);

        FrameLayout tile = createTile(selected);

        ImageView imageView = new ImageView(requireContext());
        imageView.setImageResource(resId);
        imageView.setTag(resId);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        if (!isUnlocked) {
            imageView.setAlpha(0.4f);
        }


        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(imageParams);
        tile.addView(imageView);

        if (!isUnlocked) {
            addPriceLabel(tile, getItemPrice(itemName, true));
        }

        if (!isUnlocked) {
            ImageView lockView = new ImageView(requireContext());
            lockView.setImageResource(R.drawable.lock);
            int lockSize = dpToPx(20);
            FrameLayout.LayoutParams lockParams = new FrameLayout.LayoutParams(lockSize, lockSize);
            lockParams.setMargins(dpToPx(4), dpToPx(4), 0, 0);
            lockView.setLayoutParams(lockParams);
            tile.addView(lockView);
        }

        imageView.setOnClickListener(v -> {
            int interiorResId = (int) v.getTag();
            animateTileSelect(tile);

            if (!isUnlocked) {
                showPurchaseConfirmPopup(itemName, interiorResId, true);
            } else {
                if (isInteriorSelected(interiorResId)) {
                    showRemoveConfirmPopup(interiorResId, "interior");
                } else {
                    showChangeConfirmPopup(interiorResId, true);
                }
            }
        });

        imageView.setOnLongClickListener(v -> {
            if (!isUnlocked) return false;
            ClipData data = ClipData.newPlainText("resId", v.getTag().toString());
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, null, 0);
            return true;
        });

        imageGrid.addView(tile);
    }

    private FrameLayout createTile(boolean selected) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int outerPadding = dpToPx(24);
        int itemMargin = dpToPx(10);
        int totalMargins = itemMargin * 6;
        int tileSize = (screenWidth - outerPadding - totalMargins) / 3;

        FrameLayout tile = new FrameLayout(requireContext());
        tile.setBackgroundResource(selected ? R.drawable.bg_item_tile_selected : R.drawable.bg_item_tile);

        GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
        tileParams.width = tileSize;
        tileParams.height = tileSize;
        tileParams.setMargins(itemMargin, itemMargin, itemMargin, itemMargin);
        tile.setLayoutParams(tileParams);

        tile.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        tile.setElevation(dpToPx(selected ? 6 : 4));

        return tile;
    }

    private boolean isDressSelected(int applyResId) {
        Integer currentHat = viewModel.getHat().getValue();
        Integer currentClothes = viewModel.getClothes().getValue();

        return (currentHat != null && currentHat == applyResId)
                || (currentClothes != null && currentClothes == applyResId);
    }

    private boolean isInteriorSelected(int resId) {
        Integer currentInterior = viewModel.getInterior().getValue();
        return currentInterior != null && currentInterior == resId;
    }

    private void updateCharacter(int resId) {
        String itemName = getResources().getResourceEntryName(resId);

        if (containsApplyResId(hatList, resId)) {
            viewModel.setHat(resId);
            ((MainActivity) getActivity()).updateEquippedItem("hat", itemName);
        } else if (containsApplyResId(clothesList, resId)) {
            viewModel.setClothes(resId);
            ((MainActivity) getActivity()).updateEquippedItem("clothes", itemName);
        } else if (interiorList.contains(resId)) {
            viewModel.setInterior(resId);
            ((MainActivity) getActivity()).updateEquippedItem("interior", itemName);
        }
    }

    private void removeEquippedItem(String removeType) {
        MainActivity mainActivity = (MainActivity) getActivity();

        if ("hat".equals(removeType)) {
            viewModel.setHat(0);
            if (mainActivity != null) mainActivity.updateEquippedItem("hat", "");
        } else if ("clothes".equals(removeType)) {
            viewModel.setClothes(0);
            if (mainActivity != null) mainActivity.updateEquippedItem("clothes", "");
        } else if ("interior".equals(removeType)) {
            viewModel.setInterior(R.drawable.background_hill);
            if (mainActivity != null) mainActivity.updateEquippedItem("interior", "background_hill");
        }

        refreshCurrentTab();
    }

    private boolean containsApplyResId(List<DressItem> itemList, int resId) {
        for (DressItem item : itemList) {
            if (item.getApplyResId() == resId) return true;
        }
        return false;
    }

    private void resetCharacter() {
        viewModel.reset();
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.updateEquippedItem("hat", "");
            mainActivity.updateEquippedItem("clothes", "");
            mainActivity.updateEquippedItem("interior", "background_hill");
        }
    }

    private void refreshCurrentTab() {
        if (imageGrid == null) return;

        switch (currentTab) {
            case TAB_HAT: showDressItems(hatList); break;
            case TAB_TOP: showDressItems(clothesList); break;
            case TAB_INTERIOR: showInteriorItems(interiorList); break;
            case TAB_ALL:
            default: showAllItems(); break;
        }
    }

    private void updateTabButtons() {
        styleTab(tabAllBox, currentTab == TAB_ALL);
        styleTab(tabHatBox, currentTab == TAB_HAT);
        styleTab(tabTopBox, currentTab == TAB_TOP);
        styleTab(tabInteriorBox, currentTab == TAB_INTERIOR);
    }

    private void styleTab(FrameLayout box, boolean selected) {
        box.setBackgroundResource(selected ? R.drawable.bg_message_box_selected : R.drawable.bg_message_box);
        box.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
    }

    private void animateTileSelect(View tile) {
        tile.animate().scaleX(1.06f).scaleY(1.06f).setDuration(90)
                .withEndAction(() -> tile.animate().scaleX(1f).scaleY(1f).setDuration(90).start())
                .start();
    }

    private void applyPressAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    break;
            }
            return false;
        });
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // 상점용 확인 팝업 설계 유지 결합
    private void showPurchaseConfirmPopup(String itemName, int resId, boolean isInterior) {
        resetPendingStates();
        MainActivity mainActivity = (MainActivity) getActivity();
        int currentGold = (mainActivity != null) ? mainActivity.getGold() : 0;
        int price = getItemPrice(itemName, isInterior);

        pendingIsPurchase = true;
        pendingItemName = itemName;
        pendingResId = resId;
        pendingIsInterior = isInterior;

        TextView tvConfirmMessage = changeConfirmPopup.findViewById(R.id.tvConfirmMessage);
        if (tvConfirmMessage != null) {
            if (currentGold < price) {
                tvConfirmMessage.setText("재화가 부족하여\n구매할 수 없습니다.\n(보유: " + currentGold + "G)");
            } else {
                tvConfirmMessage.setText(price + " Gold를 사용하여\n구매하시겠습니까?\n(보유: " + currentGold + "G)");
            }
        }

        openPopupAnimation();
    }

    private void showChangeConfirmPopup(int resId, boolean isInterior) {
        resetPendingStates();
        pendingResId = resId;
        pendingIsInterior = isInterior;

        TextView tvConfirmMessage = changeConfirmPopup.findViewById(R.id.tvConfirmMessage);
        if (tvConfirmMessage != null) {
            tvConfirmMessage.setText("변경할까요?");
        }
        openPopupAnimation();
    }

    private void showRemoveConfirmPopup(int resId, String removeType) {
        resetPendingStates();
        pendingResId = resId;
        pendingIsRemove = true;
        pendingRemoveType = removeType;

        TextView tvConfirmMessage = changeConfirmPopup.findViewById(R.id.tvConfirmMessage);
        if (tvConfirmMessage != null) {
            tvConfirmMessage.setText("현재 아이템을\n제거할까요?");
        }
        openPopupAnimation();
    }

    private void openPopupAnimation() {
        confirmDimView.setAlpha(0f);
        confirmDimView.setVisibility(View.VISIBLE);
        confirmDimView.animate().alpha(1f).setDuration(180).start();

        changeConfirmPopup.setVisibility(View.VISIBLE);
        changeConfirmPopup.setAlpha(0f);
        changeConfirmPopup.setScaleX(0.88f);
        changeConfirmPopup.setScaleY(0.88f);
        changeConfirmPopup.setTranslationY(20f);

        changeConfirmPopup.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(220).start();
    }

    private void hideChangeConfirmPopup(Runnable endAction) {
        confirmDimView.animate().alpha(0f).setDuration(160)
                .withEndAction(() -> {
                    confirmDimView.setVisibility(View.GONE);
                    confirmDimView.setAlpha(1f);
                }).start();

        changeConfirmPopup.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).translationY(16f).setDuration(180)
                .withEndAction(() -> {
                    changeConfirmPopup.setVisibility(View.GONE);
                    changeConfirmPopup.setAlpha(1f);
                    changeConfirmPopup.setScaleX(1f);
                    changeConfirmPopup.setScaleY(1f);
                    changeConfirmPopup.setTranslationY(0f);

                    if (endAction != null) {
                        endAction.run();
                    }
                }).start();
    }

    private void addPriceLabel(FrameLayout tile, int price) {
        TextView priceView = new TextView(requireContext());
        priceView.setText(price + "G");
        priceView.setTextSize(12);
        priceView.setTextColor(Color.WHITE);
        priceView.setTypeface(null, android.graphics.Typeface.BOLD);
        priceView.setGravity(Gravity.CENTER);
        priceView.setBackgroundResource(R.drawable.bg_price_label);
        priceView.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));

        FrameLayout.LayoutParams priceParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        priceParams.gravity = Gravity.BOTTOM | Gravity.END;
        priceParams.setMargins(0, 0, dpToPx(1), dpToPx(1));

        tile.addView(priceView, priceParams);
    }
}