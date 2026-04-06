package com.example.term_project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CharacterViewModel extends ViewModel {

    // 기본 캐릭터 몸체
    private final MutableLiveData<Integer> character = new MutableLiveData<>(R.drawable.character_base);

    // 표정
    private final MutableLiveData<Integer> face = new MutableLiveData<>(R.drawable.face_default);

    // 모자
    private final MutableLiveData<Integer> hat = new MutableLiveData<>(0);

    // 옷
    private final MutableLiveData<Integer> clothes = new MutableLiveData<>(0);

    public LiveData<Integer> getCharacter() {
        return character;
    }

    public LiveData<Integer> getFace() {
        return face;
    }

    public LiveData<Integer> getHat() {
        return hat;
    }

    public LiveData<Integer> getClothes() {
        return clothes;
    }

    public void setCharacter(int resId) {
        character.setValue(resId);
    }

    public void setFace(int resId) {
        face.setValue(resId);
    }

    public void setHat(int resId) {
        hat.setValue(resId);
    }

    public void setClothes(int resId) {
        clothes.setValue(resId);
    }

    public void reset() {
        character.setValue(R.drawable.character_base);
        face.setValue(R.drawable.face_default);
        hat.setValue(0);
        clothes.setValue(0);
    }
}