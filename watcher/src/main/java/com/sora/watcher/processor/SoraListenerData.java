package com.sora.watcher.processor;


import com.sora.watcher.config.ProcessWatchData;
import com.sora.watcher.listerner.ISoraDataListener;
import lombok.Data;
import org.springframework.aop.support.AopUtils;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;



@Data
public class SoraListenerData {
    private ISoraDataListener listener;
    private String name;
    private String dataBase;
    private String table;
    private Class<?> entityClass;

    public SoraListenerData(ISoraDataListener listener, ProcessWatchData config) {
        this.listener = listener;

        name = config.getName();
        table = config.getTableName();
        dataBase = config.getDataBase();
        Class<?> targetClass = AopUtils.getTargetClass(listener);
        entityClass = getGenericClass(targetClass);
    }


    private Class<?> getGenericClass(Class<?> targetClass) {
        if (targetClass == Object.class)
            return null;

        Type[] types = targetClass.getGenericInterfaces();
        if (types.length == 0) {
            types = new Type[] {targetClass.getGenericSuperclass()};
        }

        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                ParameterizedType t = (ParameterizedType) type;
                Type[] array = t.getActualTypeArguments();
                return (Class<?>) array[0];
            }
        }

        return getGenericClass(targetClass.getSuperclass());
    }
}
