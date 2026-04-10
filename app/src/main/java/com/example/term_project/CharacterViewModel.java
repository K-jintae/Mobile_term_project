package com.example.term_project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CharacterViewModel extends ViewModel {

    private final MutableLiveData<Integer> character = new MutableLiveData<>(R.drawable.character_base);
    private final MutableLiveData<Integer> face = new MutableLiveData<>(R.drawable.face_default);
    private final MutableLiveData<Integer> hat = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> clothes = new MutableLiveData<>(0);

    // 추가
    private final MutableLiveData<Integer> interior = new MutableLiveData<>(R.drawable.background_hill);

    public LiveData<Integer> getCharacter() { return character; }
    public LiveData<Integer> getFace() { return face; }
    public LiveData<Integer> getHat() { return hat; }
    public LiveData<Integer> getClothes() { return clothes; }
    public LiveData<Integer> getInterior() { return interior; }

    public void setCharacter(int resId) { character.setValue(resId); }
    public void setFace(int resId) { face.setValue(resId); }
    public void setHat(int resId) { hat.setValue(resId); }
    public void setClothes(int resId) { clothes.setValue(resId); }
    public void setInterior(int resId) { interior.setValue(resId); }

    public void reset() {
        character.setValue(R.drawable.character_base);
        face.setValue(R.drawable.face_default);
        hat.setValue(0);
        clothes.setValue(0);
        interior.setValue(R.drawable.background_hill);
    }
}