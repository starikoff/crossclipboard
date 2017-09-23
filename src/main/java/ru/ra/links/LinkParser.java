package ru.ra.links;

import java.net.URL;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ning.http.client.Response;

import ru.ra.Environment;
import ru.ra.util.Futurizer;
import ru.ra.util.LinkUtil;

public class LinkParser {
    private static final Logger log = LoggerFactory.getLogger(LinkParser.class);

    public static String getServer(URL url) {
        final String proto = url.getProtocol();
        if (StringUtils.lowerCase(proto).startsWith("http")) {
            String server;
            if (url.getPort() >= 0) {
                server = String.format("%s:%d", url.getHost(), url.getPort());
            } else {
                server = String.format("%s", url.getHost());
            }
            return server;
        } else {
            return null;
        }
    }

    public static ListenableFuture<LinkInfo> parseLinkInfo(final URL url) {
        if (url == null) {
            return Futures.immediateFuture(null);
        } else {
            final ListenableFuture<HtmlInfo> htmlInfoFuture = getHtmlInfo(url);
            final ListenableFuture<LinkInfo> linkInfoFuture =
                Environment.getPublished(Futurizer.class).transform(htmlInfoFuture,
                    new AsyncFunction<HtmlInfo, LinkInfo>() {
                        @Override
                        public ListenableFuture<LinkInfo> apply(
                                final HtmlInfo htmlInfo) {
                            final String proto = url.getProtocol();
                            if (htmlInfo != null
                                && StringUtils.lowerCase(proto).startsWith(
                                    "http")) {
                                final String server;
                                if (url.getPort() >= 0) {
                                    server =
                                        String.format("%s:%d", url.getHost(),
                                            url.getPort());
                                } else {
                                    server = String.format("%s", url.getHost());
                                }
                                final String title =
                                    StringUtils.defaultString(htmlInfo.title,
                                        url.toString());
                                final FaviconInfo faviconInfo =
                                    faviconUrl(htmlInfo, proto, server);
                                if (faviconInfo.guessing) {
                                    // we are guessing, so let's try it
                                    ListenableFuture<Response> resp =
                                        TitleExtractor
                                            .reqHead(faviconInfo.faviconUrl);
                                    return Environment.getPublished(Futurizer.class)
                                        .transform(resp,
                                            new Function<Response, LinkInfo>() {
                                                @Override
                                                public LinkInfo apply(
                                                        Response resp) {
                                                    String faviconConfirmedUrl =
                                                        null;
                                                    if (resp.getStatusCode() / 100 == 2) {
                                                        faviconConfirmedUrl =
                                                            faviconInfo.faviconUrl;
                                                    }
                                                    return new LinkInfo(null,
                                                        true, url.toString(),
                                                        title, server,
                                                        faviconConfirmedUrl);
                                                }
                                            });
                                } else
                                    return Futures
                                        .immediateFuture(new LinkInfo(null,
                                            true, url.toString(), title,
                                            server, faviconInfo.faviconUrl));
                            } else {
                                return Futures.immediateFuture(new LinkInfo(
                                    null, true, url.toString(), null, null,
                                    null));
                            }
                        }
                    });
            return Environment.getPublished(Futurizer.class).withFallback(
                linkInfoFuture, new FutureFallback<LinkInfo>() {
                    @Override
                    public ListenableFuture<LinkInfo> create(Throwable t)
                            throws Exception {
                        log.error("fallback error", t);
                        return Futures.immediateFuture(new LinkInfo(null, true,
                            url.toString(), null, null, null));
                    }
                });
        }
    }

    private static class HeadDetails {
        public final int statusCode;
        public final Optional<String> contentType;
        public final Optional<Integer> size;

        private HeadDetails(int statusCode, Optional<String> contentType,
                Optional<Integer> size) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.size = size;
        }
    }

    static class HtmlInfo {
        public final String title;

        public final String faviconIconUrl;

        public HtmlInfo(String title, String faviconIconUrl) {
            this.title = title;
            this.faviconIconUrl = faviconIconUrl;
        }
    }

    private static ListenableFuture<HtmlInfo> getHtmlInfo(final URL url) {
        final String proto = url.getProtocol();
        if (!StringUtils.lowerCase(proto).startsWith("http")) {
            return Futures.immediateFuture(null);
        }
        try {
            ListenableFuture<Response> headFuture = TitleExtractor.reqHead(url);
            ListenableFuture<HeadDetails> headResultsFuture =
                parseHeadResponse(headFuture);
            ListenableFuture<String> htmlFuture =
                maybeGet(url, headResultsFuture);
            ListenableFuture<String> titleFuture = extractTitle(htmlFuture);
            ListenableFuture<String> faviconImgUrlFuture =
                extractFaviconUrl(htmlFuture);
            ListenableFuture<List<String>> titleAndImgListFuture =
                Futures.allAsList(titleFuture, faviconImgUrlFuture);
            ListenableFuture<HtmlInfo> result =
                asHtmlInfo(titleAndImgListFuture);
            return result;
        } catch (final Exception e) {
            log.error("error getting title for " + url, e);
        }
        return Futures.immediateFuture(null);
    }

    private static ListenableFuture<HtmlInfo> asHtmlInfo(
            ListenableFuture<List<String>> titleAndImgListFuture) {
        return Environment.getPublished(Futurizer.class).transform(
            titleAndImgListFuture, new Function<List<String>, HtmlInfo>() {
                @Override
                public HtmlInfo apply(List<String> input) {
                    if (input.size() == 2) {
                        return new HtmlInfo(input.get(0), input.get(1));
                    } else {
                        return null;
                    }
                }
            });
    }

    private static ListenableFuture<String> extractFaviconUrl(
            ListenableFuture<String> htmlFuture) {
        return Environment.getPublished(Futurizer.class).transform(htmlFuture,
            new Function<String, String>() {
                @Override
                public String apply(String html) {
                    return TitleExtractor.extractImage(html);
                }
            });
    }

    private static ListenableFuture<String> extractTitle(
            ListenableFuture<String> htmlFuture) {
        return Environment.getPublished(Futurizer.class).transform(htmlFuture,
            new Function<String, String>() {
                @Override
                public String apply(String html) {
                    return TitleExtractor.extractTitle(html);
                }
            });
    }

    private static ListenableFuture<String> maybeGet(final URL url,
            ListenableFuture<HeadDetails> headResultsFuture) {
        return Environment.getPublished(Futurizer.class).transform(headResultsFuture,
            new AsyncFunction<HeadDetails, String>() {
                @Override
                public ListenableFuture<String> apply(@Nonnull HeadDetails headDet) {
                    if (headDet.statusCode != 200) {
                        return Futures
                            .immediateFailedFuture(new IllegalStateException(
                                "HEAD status " + headDet.statusCode));
                    }
                    if (headDet.contentType.isPresent()) {
                        if (headDet.contentType.get().startsWith("text/html")) {
                            Optional<Integer> sizeOpt = headDet.size;
                            if (!sizeOpt.isPresent()
                                || sizeOpt.get() < 500 * 1024) {
                                return TitleExtractor.load(url);
                            } else {
                                log.debug("not loading URL "
                                    + url.toExternalForm() + ": size is "
                                    + sizeOpt.get());
                            }
                        } else {
                            log.debug("not loading URL " + url.toExternalForm()
                                + ": content type is "
                                + headDet.contentType.get());
                        }
                    }
                    return Futures.immediateFuture(null);
                }
            });
    }

    private static ListenableFuture<HeadDetails> parseHeadResponse(
            ListenableFuture<Response> headFuture) {
        return Environment.getPublished(Futurizer.class).transform(headFuture,
            new Function<Response, HeadDetails>() {
                @Override
                public HeadDetails apply(Response headResp) {
                    String contentLengthStr =
                        headResp.getHeader("Content-Length");
                    Optional<Integer> sizeOpt;
                    try {
                        sizeOpt =
                            Optional.of(Integer.valueOf(contentLengthStr));
                    } catch (Exception e) {
                        sizeOpt = Optional.absent();
                    }
                    log.debug("head status " + headResp.getStatusCode());
                    log.debug("head size " + sizeOpt.toString());
                    Optional<String> contentTypeOpt;
                    String ct = headResp.getContentType();
                    if (!StringUtils.isBlank(ct)) {
                        contentTypeOpt = Optional.of(ct);
                    } else {
                        contentTypeOpt = Optional.absent();
                    }
                    log.debug("head content type " + contentTypeOpt.toString());
                    return new HeadDetails(headResp.getStatusCode(),
                        contentTypeOpt, sizeOpt);
                }
            });
    }

    private static class FaviconInfo {
        public final String faviconUrl;

        public final boolean guessing;

        private FaviconInfo(String faviconUrl, boolean guessing) {
            this.faviconUrl = faviconUrl;
            this.guessing = guessing;
        }

        static FaviconInfo of(String faviconUrl, boolean guessing) {
            return new FaviconInfo(faviconUrl, guessing);
        }
    }

    private static FaviconInfo faviconUrl(final HtmlInfo htmlInfo,
            final String proto, final String server) {
        String faviconUrl = htmlInfo.faviconIconUrl;
        if (faviconUrl != null) {
            if (LinkUtil.isLink(faviconUrl)) {
                return FaviconInfo.of(faviconUrl, false);
            } else if (faviconUrl.startsWith("//")) {
                return FaviconInfo.of(proto + ":" + faviconUrl, false);
            } else if (faviconUrl.startsWith("/")) {
                return FaviconInfo.of(proto + "://" + server + faviconUrl,
                    false);
            } else {
                // FIXME: not really, app path omitted
                return FaviconInfo.of(
                    proto + "://" + server + "/" + faviconUrl, true);
            }
        }
        return FaviconInfo.of(proto + "://" + server + "/favicon.ico", true);
    }
}
