package com.wilbert.sveditor.library.utils.listenableList;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wilbert
 * @Date 2020/7/2 15:54
 * @email jw20082009@qq.com
 **/
public class EmptyListenable<E> extends ListListenable<IEmptyListener> {
    AtomicBoolean mIsEmpty = new AtomicBoolean(true);
    List<E> mListenableList = null;

    public void attachList(List<E> list) {
        mListenableList = list;
    }

    protected void notifyEmpty() {
        if (mIsEmpty.get()) {
            return;
        }
        if (mListeners != null) {
            for (IEmptyListener listener : mListeners) {
                listener.onEmpty();
            }
        }
        mIsEmpty.set(true);
    }

    protected void notifyFirstElements() {
        if (!mIsEmpty.get()) {
            return;
        }
        if (mListeners != null) {
            for (IEmptyListener listener : mListeners) {
                listener.onEmpty();
            }
        }
        mIsEmpty.set(false);
    }

    protected void addAndNotify(E unit) {
        if (mListenableList == null || unit == null) return;
        boolean isEmpty = mListenableList.isEmpty();
        mListenableList.add(unit);
        if (isEmpty) {
            notifyFirstElements();
        }
    }

    protected void removeAndNotify(E unit) {
        if (mListenableList == null || unit == null || mListenableList.isEmpty()) return;
        mListenableList.remove(unit);
        if (mListenableList.isEmpty()) {
            notifyEmpty();
        }
    }

    protected void removeAndNotifyByCondition(IRemoveCondition<E> unitsCondition) {
        if (mListenableList == null || mListenableList.isEmpty()) return;
        for (Iterator iter = mListenableList.iterator(); iter.hasNext(); ) {
            E unit = (E) iter.next();
            if (unitsCondition.needRemove(unit)) {
                iter.remove();
            }
        }
        if (mListenableList.isEmpty()) {
            notifyEmpty();
        }
    }

    interface IRemoveCondition<T> {
        boolean needRemove(T t);
    }

    public boolean isEmpty() {
        return mIsEmpty.get();
    }
}
