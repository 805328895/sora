package com.sora.watcher.listerner;

import com.sora.watcher.config.SoraModel;

public interface ISoraDataListener<T> {
    void onUpdate(T from, T to);
    void onInsert(T data);
    void onDelete(T data);
    void savePosition(String key, SoraModel soraModel);

    /**
     *
     * @param from
     * @param to
     * @param type   1 新增，2 修改
     */
    Boolean saveErrorData(T from, T to,Integer type,String msg);
}
