package com.wilbert.sveditor.library.utils.listenableList;

import java.util.ArrayList;
import java.util.List;

public class ListListenable<T> {

    protected List<T> mListeners = new ArrayList<>();

    public void addListener(T listener) {
        if (listener == null)
            return;
        mListeners.add(listener);
    }

    public void removeListener(T listener) {
        if (listener == null || mListeners.isEmpty())
            return;
        mListeners.remove(listener);
    }

    public void clearListeners() {
        mListeners.clear();
    }
}
