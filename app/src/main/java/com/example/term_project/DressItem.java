package com.example.term_project;

public class DressItem {
    private final int previewResId;   // 선택창에서 보일 이미지
    private final int applyResId;     // 실제 캐릭터에 적용할 이미지

    public DressItem(int previewResId, int applyResId) {
        this.previewResId = previewResId;
        this.applyResId = applyResId;
    }

    public int getPreviewResId() {
        return previewResId;
    }

    public int getApplyResId() {
        return applyResId;
    }
}