package ru.ra.util;

import java.net.MalformedURLException;
import java.net.URL;

public class LinkUtil {

    public static String urlize(String maybeUrl) {
        if (!maybeUrl.contains("://")) {
            maybeUrl = "http://" + maybeUrl;
        }
        return maybeUrl;
    }

    public static boolean isLink(String newUrl) {
        try {
            new URL(newUrl);
            return true;
        } catch (MalformedURLException malUrlExc) {
            return false;
        }
    }

}
