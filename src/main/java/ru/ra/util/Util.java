package ru.ra.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public class Util {
    public static String getCookieValue(HttpServletRequest req,
            String cookieName) {
        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                String name = cookie.getName();
                if (StringUtils.equals(name, cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String getAuth(HttpServletRequest req) {
        return getCookieValue(req, "auth-info");
    }

    public static boolean equal(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return (o1 == null && o2 == null);
        }
        return o1.equals(o2);
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    public static int hash(Object... objects) {
        int result = 37;
        for (Object o : objects) {
            result = result * 13 + hashCode(o);
        }
        return result;
    }
}
