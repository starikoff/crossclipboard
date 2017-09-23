package ru.ra.links;

public class LinkInfo {
    public final String id;

    public final String faviconUrl;
    
    public final boolean isUrl;

    public final String content;

    public final String title;

    public final String server;

    public LinkInfo(final String id, final boolean isUrl, final String content, final String title,
            final String server, final String faviconUrl) {
        this.id = id;
        this.isUrl = isUrl;
        this.faviconUrl = faviconUrl;
        this.content = content;
        this.title = title;
        this.server = server;
    }
}
