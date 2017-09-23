package ru.ra.util;

import org.apache.commons.lang3.StringUtils;

public class EnumUtil {
    public static <E extends Enum<E>> E fromName(Class<E> ecls, String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return Enum.valueOf(ecls, name);
    }

    public static <E extends Enum<E>> String nameOf(E e) {
        if (e == null) {
            return null;
        }
        return e.name();
    }
}
