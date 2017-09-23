package ru.ra.data;

import ru.ra.util.Util;

public class LinkData {
    public String id;

    public final String url;

    public final String title;

    public LinkData(String id, String url, String title) {
        this.id = id;
        this.url = url;
        this.title = title;
    }

    @Override
    public int hashCode() {
        return Util.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LinkData)) {
            return false;
        }
        LinkData that = (LinkData) obj;
        return Util.equal(this.id, that.id);
    }
}
