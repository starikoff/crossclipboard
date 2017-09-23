package ru.ra;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class Environment {

    private static final Logger log = Logger.getLogger(Environment.class);

    private static final ConcurrentHashMap<Class<?>, Object> map =
        new ConcurrentHashMap<>();

    public static <T> T getPublished(Class<T> cls) {
        if (!map.containsKey(cls)) {
            throw new UnpublishedException(cls);
        }
        return cls.cast(map.get(cls));
    }

    public static <T> T publish(Class<? super T> cls, T obj) {
        map.put(cls, cls.cast(obj));
        return obj;
    }

    public static void dispose() {
        for (Entry<Class<?>, Object> ent : map.entrySet()) {
            Object o = ent.getValue();
            if (o instanceof IDisposable) {
                ((IDisposable) o).dispose();
                log.info(o + " disposed");
            }
        }
    }

    public static final class UnpublishedException extends RuntimeException {
        public UnpublishedException(Class<?> cls) {
            super("object of type " + cls + " is not published");
        }
    }

    public interface IDisposable {
        void dispose();
    }
}
