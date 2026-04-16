package com.example.term_project;

import android.content.ClipData;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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

    // 확인 팝업용
    private View confirmDimView;
    private LinearLayout changeConfirmPopup;
    private AppCompatButton btnConfirmYes, btnConfirmNo;

    private int pendingResId = 0;
    private boolean pendingIsInterior = false;

    private static final int TAB_ALL = 0;
    private static final int TAB_HAT = 1;
    private static final int TAB_TOP = 2;
    private static final int TAB_INTERIOR = 3;

    private int currentTab = TAB_ALL;

    private final List<DressItem> hatList = Arrays.asList(
            new DressItem(R.drawable.thumb_hat_halloween, R.drawable.hat_halloween),
            new DressItem(R.drawable.thumb_hat_hiphop, R.drawable.hat_hiphop),
            new DressItem(R.drawable.thumb_hat_onepiece, R.drawable.hat_onepiece),
            new DressItem(R.drawable.thumb_hat_crown, R.drawable.hat_crown),
            new DressItem(R.drawable.thumb_hat_rabbit, R.drawable.hat_rabbit),
            new DressItem(R.drawable.thumb_hat_pokemon, R.drawable.hat_pokemon),
            new DressItem(R.drawable.thumb_hat_santa, R.drawable.hat_santa)
    );

    private final List<DressItem> clothesList = Arrays.asList(
            new DressItem(R.drawable.thumb_clothes_halloween, R.drawable.clothes_halloween),
            new DressItem(R.drawable.thumb_clothes_hiphop, R.drawable.clothes_hiphop),
            new DressItem(R.drawable.thumb_clothes_onepiece, R.drawable.clothes_onepiece),
            new DressItem(R.drawable.thumb_clothes_pokemon, R.drawable.clothes_pokemon),
            new DressItem(R.drawable.thumb_clothes_santa, R.drawable.clothes_santa),
            new DressItem(R.drawable.thumb_clothes_hoodie, R.drawable.clothes_hoodie),
            new DressItem(R.drawable.thumb_clothes_poor, R.drawable.clothes_poor),
            new DressItem(R.drawable.thumb_clothes_rabbit, R.drawable.clothes_rabbit)
    );

    private final List<Integer> interiorList = Arrays.asList(
            R.drawable.background_hill,
            R.drawable.background_room,
            R.drawable.background_space
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_right, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(CharacterViewModel.class);

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

        // 확인 팝업 연결
        confirmDimView = view.findViewById(R.id.confirmDimView);
        changeConfirmPopup = view.findViewById(R.id.changeConfirmPopup);
        btnConfirmYes = view.findViewById(R.id.btnConfirmYes);
        btnConfirmNo = view.findViewById(R.id.btnConfirmNo);

        applyPressAnimation(btnReset);
        applyPressAnimation(tabAllBox);
        applyPressAnimation(tabHatBox);
        applyPressAnimation(tabTopBox);
        applyPressAnimation(tabInteriorBox);

        topArea.setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                ClipData.Item item = event.getClipData().getItemAt(0);
                int resId = Integer.parseInt(item.getText().toString());
                updateCharacter(resId);
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

        btnConfirmYes.setOnClickListener(v -> {
            hideChangeConfirmPopup(() -> {
                if (pendingIsInterior) {
                    viewModel.setInterior(pendingResId);
                } else {
                    updateCharacter(pendingResId);
                }
            });
        });

        btnConfirmNo.setOnClickListener(v -> hideChangeConfirmPopup(null));

        viewModel.getFace().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                faceImage.setImageResource(resId);
            } else {
                faceImage.setImageDrawable(null);
            }
        });

        viewModel.getHat().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                hatImage.setImageResource(resId);
            } else {
                hatImage.setImageDrawable(null);
            }
            refreshCurrentTab();
        });

        viewModel.getClothes().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                clothesImage.setImageResource(resId);
            } else {
                clothesImage.setImageDrawable(null);
            }
            refreshCurrentTab();
        });

        viewModel.getInterior().observe(getViewLifecycleOwner(), resId -> {
            bgPreview.setImageResource(resId);
            refreshCurrentTab();
        });

        updateTabButtons();
        showAllItems();

        return view;
    }

    private void showAllItems() {
        if (imageGrid == null) return;
        imageGrid.removeAllViews();

        for (DressItem item : hatList) {
            addDressTile(item);
        }

        for (DressItem item : clothesList) {
            addDressTile(item);
        }

        for (int resId : interiorList) {
            addInteriorTile(resId);
        }
    }

    private void showDressItems(List<DressItem> itemList) {
        if (imageGrid == null) return;
        imageGrid.removeAllViews();

        for (DressItem item : itemList) {
            addDressTile(item);
        }
    }

    private void showInteriorItems(List<Integer> imageList) {
        if (imageGrid == null) return;
        imageGrid.removeAllViews();

        for (int resId : imageList) {
            addInteriorTile(resId);
        }
    }

    private void addDressTile(DressItem item) {
        boolean selected = isDressSelected(item.getApplyResId());
        FrameLayout tile = createTile(selected);

        ImageView imageView = new ImageView(requireContext());
        imageView.setImageResource(item.getPreviewResId());
        imageView.setTag(item.getApplyResId());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(imageParams);

        imageView.setOnClickListener(v -> {
            int resId = (int) v.getTag();
            animateTileSelect(tile);
            showChangeConfirmPopup(resId, false);
        });

        imageView.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("resId", v.getTag().toString());
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, null, 0);
            return true;
        });

        tile.addView(imageView);
        imageGrid.addView(tile);
    }

    private void addInteriorTile(int resId) {
        boolean selected = isInteriorSelected(resId);
        FrameLayout tile = createTile(selected);

        ImageView imageView = new ImageView(requireContext());
        imageView.setImageResource(resId);
        imageView.setTag(resId);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(imageParams);

        imageView.setOnClickListener(v -> {
            int interiorResId = (int) v.getTag();
            animateTileSelect(tile);
            showChangeConfirmPopup(interiorResId, true);
        });

        imageView.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("resId", v.getTag().toString());
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, null, 0);
            return true;
        });

        tile.addView(imageView);
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
        if (containsApplyResId(hatList, resId)) {
            viewModel.setHat(resId);
        } else if (containsApplyResId(clothesList, resId)) {
            viewModel.setClothes(resId);
        } else if (interiorList.contains(resId)) {
            viewModel.setInterior(resId);
        }
    }

    private boolean containsApplyResId(List<DressItem> itemList, int resId) {
        for (DressItem item : itemList) {
            if (item.getApplyResId() == resId) {
                return true;
            }
        }
        return false;
    }

    private void resetCharacter() {
        viewModel.reset();
    }

    private void refreshCurrentTab() {
        if (imageGrid == null) return;

        switch (currentTab) {
            case TAB_HAT:
                showDressItems(hatList);
                break;
            case TAB_TOP:
                showDressItems(clothesList);
                break;
            case TAB_INTERIOR:
                showInteriorItems(interiorList);
                break;
            case TAB_ALL:
            default:
                showAllItems();
                break;
        }
    }

    private void updateTabButtons() {
        styleTab(tabAllBox, currentTab == TAB_ALL);
        styleTab(tabHatBox, currentTab == TAB_HAT);
        styleTab(tabTopBox, currentTab == TAB_TOP);
        styleTab(tabInteriorBox, currentTab == TAB_INTERIOR);
    }

    private void styleTab(FrameLayout box, boolean selected) {
        if (selected) {
            box.setBackgroundResource(R.drawable.bg_message_box_selected);
            box.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(120)
                    .start();
        } else {
            box.setBackgroundResource(R.drawable.bg_message_box);
            box.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(120)
                    .start();
        }
    }

    private void animateTileSelect(View tile) {
        tile.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(90)
                .withEndAction(() -> tile.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(90)
                        .start())
                .start();
    }

    private void applyPressAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(80)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .start();
                    break;
            }
            return false;
        });
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showChangeConfirmPopup(int resId, boolean isInterior) {
        pendingResId = resId;
        pendingIsInterior = isInterior;

        confirmDimView.setAlpha(0f);
        confirmDimView.setVisibility(View.VISIBLE);
        confirmDimView.animate()
                .alpha(1f)
                .setDuration(180)
                .start();

        changeConfirmPopup.setVisibility(View.VISIBLE);
        changeConfirmPopup.setAlpha(0f);
        changeConfirmPopup.setScaleX(0.88f);
        changeConfirmPopup.setScaleY(0.88f);
        changeConfirmPopup.setTranslationY(20f);

        changeConfirmPopup.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
    }

    private void hideChangeConfirmPopup(Runnable endAction) {
        confirmDimView.animate()
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    confirmDimView.setVisibility(View.GONE);
                    confirmDimView.setAlpha(1f);
                })
                .start();

        changeConfirmPopup.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(16f)
                .setDuration(180)
                .withEndAction(() -> {
                    changeConfirmPopup.setVisibility(View.GONE);
                    changeConfirmPopup.setAlpha(1f);
                    changeConfirmPopup.setScaleX(1f);
                    changeConfirmPopup.setScaleY(1f);
                    changeConfirmPopup.setTranslationY(0f);

                    if (endAction != null) {
                        endAction.run();
                    }
                })
                .start();
    }
}