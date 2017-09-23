package ru.ra.links;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.ning.http.client.extra.ListenableFutureAdapter;

public class TitleExtractor {

    private static final Logger LOG = LoggerFactory
        .getLogger(TitleExtractor.class);

    private static final AsyncHttpClient httpClient;

    static {
        final AsyncHttpClientConfig.Builder configBuilder =
            new AsyncHttpClientConfig.Builder();
        configBuilder.setFollowRedirects(true);
        configBuilder.setAllowPoolingConnection(true);
        configBuilder.setAllowSslConnectionPool(true);
        configBuilder.setCompressionEnabled(true);
        configBuilder.setIOThreadMultiplier(1);
        configBuilder.setConnectionTimeoutInMs(9000);
        configBuilder.setRequestTimeoutInMs(9000);
        configBuilder.setStrict302Handling(true);
        configBuilder
            .setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36");
        httpClient = new AsyncHttpClient(configBuilder.build());
    }

    public static ListenableFuture<Response> reqHead(final URL url) {
        return reqHead(url.toExternalForm());
    }

    public static ListenableFuture<Response> reqHead(final String urlStr) {
        try {
            com.ning.http.client.ListenableFuture<Response> head =
                httpClient.prepareHead(urlStr).execute(
                    new AsyncCompletionHandler<Response>() {
                        @Override
                        public Response onCompleted(final Response paramResponse)
                                throws Exception {
                            LOG.debug("complete " + urlStr);
                            return paramResponse;
                        }

                        @Override
                        public void onThrowable(final Throwable t) {
                            LOG.error("error", t);
                        }
                    });
            ListenableFuture<Response> headFuture =
                ListenableFutureAdapter.asGuavaFuture(head);
            return headFuture;
        } catch (IOException e) {
            LOG.error("error sending HEAD to " + urlStr);
            return Futures.immediateFailedFuture(e);
        }
    }

    public static ListenableFuture<String> load(final URL url) {
        if (url == null) {
            return Futures.immediateFuture(null);
        }
        LOG.debug("fetching title for url " + url);
        try {
            com.ning.http.client.ListenableFuture<Response> result =
                httpClient.prepareGet(url.toExternalForm()).execute(
                    new AsyncCompletionHandler<Response>() {
                        @Override
                        public Response onCompleted(final Response paramResponse)
                                throws Exception {
                            return paramResponse;
                        }

                        @Override
                        public void onThrowable(final Throwable t) {
                            LOG.error("error", t);
                        }
                    });
            return Futures.transform(
                ListenableFutureAdapter.asGuavaFuture(result),
                new Function<Response, String>() {
                    @Override
                    public String apply(final Response resp) {
                        try {
                            if (resp == null) {
                                return null;
                            }
                            String html = resp.getResponseBody();
                            String charset = getCharset(html);
                            if (charset == null) {
                                return html;
                            }
                            try {
                                return resp.getResponseBody(charset);
                            } catch (Exception e) {
                                return html;
                            }
                        } catch (final IOException e) {
                            LOG.error("error reading response", e);
                            return null;
                        }
                    }
                });
        } catch (IOException e) {
            LOG.error("error reading from url " + url.toExternalForm(), e);
            return Futures.immediateFuture(null);
        }
    }

    /**
     * Handles both &lt;meta charset="UTF-8"&gt; and e.g. &lt;meta
     * http-equiv="Content-Type" content="text/html; charset=UTF-8"&gt;
     */
    private static final Pattern p = Pattern.compile(
        "<\\s*meta[^>]+charset\\s*=\\s*\"?\\s*([^\"\\s/>]+).*>",
        Pattern.CASE_INSENSITIVE);

    public static String getCharset(String htmlPart) {
        if (htmlPart == null) {
            return null;
        }
        Matcher m = p.matcher(htmlPart);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static String extractTitle(final String html) {
        Element head = extractHead(html);
        if (head == null) {
            return null;
        }
        Element titleEl = head.select("title").first();
        if (titleEl == null) {
            return null;
        }
        return titleEl.text();
    }

    protected static Element extractHead(final String html) {
        if (html == null) {
            return null;
        }
        Document htmlDoc = Jsoup.parse(html);
        Element head = htmlDoc.select("head").first();
        return head;
    }

    public static String extractImage(String html) {
        Element head = extractHead(html);
        if (head == null) {
            return null;
        }
        Element shortcutIconEl =
            head.select("link[rel=shortcut icon][href]").first();
        if (shortcutIconEl != null) {
            return shortcutIconEl.attr("href");
        }
        Element iconEl = head.select("link[rel=icon][href]").first();
        if (iconEl != null) {
            return iconEl.attr("href");
        }
        return null;
    }
}
