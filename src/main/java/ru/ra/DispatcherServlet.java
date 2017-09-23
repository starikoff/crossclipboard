package ru.ra;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

import ru.ra.errors.ContentReadException;
import ru.ra.errors.ContentTooLargeException;
import ru.ra.errors.ContentWriteException;
import ru.ra.errors.LoginValidationException;
import ru.ra.errors.PasswordValidationException;
import ru.ra.errors.UnauthorizedException;
import ru.ra.links.LinkInfo;
import ru.ra.links.LinkParser;
import ru.ra.storage.ILink;
import ru.ra.storage.ILogicalStorage;
import ru.ra.storage.INote;
import ru.ra.storage.INote.INoteCoord;
import ru.ra.util.Futurizer;
import ru.ra.util.LinkUtil;
import ru.ra.util.Util;

@WebServlet(urlPatterns = "/", asyncSupported = true)
public class DispatcherServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory
        .getLogger(DispatcherServlet.class);

    private final ILogicalStorage contentStorage;

    private final IAuthService authSvc;

    private final KaptchaEngine kaptchaEngine = new KaptchaEngine();

    public DispatcherServlet() {
        contentStorage = Environment.getPublished(ILogicalStorage.class);
        authSvc = Environment.getPublished(IAuthService.class);
    }

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp) throws ServletException,
            IOException {
        req.setCharacterEncoding("UTF-8");
        final Map<String, String[]> map = req.getParameterMap();
        if (map.keySet().contains("logout")) {
            final Cookie cookie = new Cookie("auth-info", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            resp.addCookie(cookie);
            resp.sendRedirect("");
        } else if (map.keySet().contains("delete")) {
            final String fullToken = Util.getAuth(req);
            try {
                final AuthInfo auth = authSvc.loginByToken(fullToken);
                LOG.debug("delete");
                String[] linkIds = map.get("id");
                try {
                    for (final String linkId : linkIds) {
                        contentStorage.remove(new INoteCoord() {
                            @Override
                            public AuthInfo getOwner() {
                                return auth;
                            }

                            @Override
                            public String getId() {
                                return linkId;
                            }
                        });
                    }
                    resp.sendRedirect("");
                } catch (ContentWriteException e) {
                    try {
                        LOG.error(
                            "could not write content for " + auth.getLogin()
                                + " " + auth.getToken(), e);
                        List<INote> notes = contentStorage.get(auth);
                        contentSync(req, resp, notes, auth.getLogin(),
                            Collections
                                .singletonList("error while deleting the item"));
                    } catch (ContentReadException cre) {
                        LOG.error(
                            "could not write content for " + auth.getLogin()
                                + " " + auth.getToken(), e);
                        contentSync(req, resp, Collections.<INote> emptyList(),
                            auth.getLogin(),
                            Collections
                                .singletonList("error while deleting the item"));
                    }
                }
            } catch (UnauthorizedException unauthExc) {
                req.setAttribute("errors",
                    Collections.singletonList("please log in first"));
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            }
        } else if (map.keySet().contains("captcha")) {
            kaptchaEngine.captcha(req, resp);
        } else {
            final String fullToken = Util.getAuth(req);
            try {
                final AuthInfo authInfo = authSvc.loginByToken(fullToken);
                LOG.debug("get");
                try {
                    List<INote> notes = contentStorage.get(authInfo);
                    contentSync(req, resp, notes, authInfo.getLogin(), null);
                } catch (final ContentReadException e) {
                    LOG.error(
                        "could not get content for " + authInfo.getLogin()
                            + " " + authInfo.getToken(), e);
                    contentSync(req, resp, Collections.<INote> emptyList(),
                        authInfo.getLogin(),
                        Collections
                            .singletonList("could not load your content"));
                }
            } catch (final UnauthorizedException ue) {
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            }
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req,
            final HttpServletResponse resp) throws ServletException,
            IOException {
        req.setCharacterEncoding("UTF-8");
        final String action = req.getParameter("action");
        if (StringUtils.equals(action, "content")) {
            // if we are sending content
            final String fullToken = Util.getAuth(req);
            try {
                final AuthInfo authInfo = authSvc.loginByToken(fullToken);
                List<INote> notes = Collections.emptyList();
                try {
                    final String content =
                        sanitize(req.getParameter("content"));
                    notes = contentStorage.get(authInfo);
                    INote note = contentStorage.addNote(authInfo, content);
                    if (LinkUtil.isLink(LinkUtil.urlize(content))) {
                        contentAsync(req, resp, authInfo, notes, note);
                    } else {
                        List<INote> allNotes =
                            ImmutableList.<INote> builder().addAll(notes)
                                .add(note).build();
                        contentSync(req, resp, allNotes, authInfo.getLogin(),
                            null);
                    }
                } catch (ContentReadException cre) {
                    LOG.error(
                        "could not read content for " + authInfo.getLogin()
                            + " " + authInfo.getToken(), cre);
                    contentSync(req, resp, notes, authInfo.getLogin(),
                        Collections.singletonList("could not read content"));
                } catch (ContentWriteException e) {
                    LOG.error(
                        "could not write content for " + authInfo.getLogin()
                            + " " + authInfo.getToken(), e);
                    contentSync(req, resp, notes, authInfo.getLogin(),
                        Collections.singletonList("could not change content"));
                } catch (ContentTooLargeException e) {
                    contentSync(req, resp, notes, authInfo.getLogin(),
                        Collections.singletonList(e.getMessage()));
                }
            } catch (final UnauthorizedException ue) {
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            }
        } else { // we are logging in
            final String login = req.getParameter("login");
            final String psw = req.getParameter("psw");
            final String captcha = req.getParameter("captcha");
            final List<String> errors = new ArrayList<>();
            if (StringUtils.isEmpty(login)) {
                errors.add("login cannot be empty");
            }
            if (StringUtils.isEmpty(psw)) {
                errors.add("password cannot be empty");
            }
            if (!validCaptcha(kaptchaEngine, captcha, req)) {
                errors.add("wrong captcha");
            }
            AuthInfo authInfo = null;
            if (errors.isEmpty()) {
                try {
                    authInfo = authSvc.login(login, psw);
                } catch (final UnauthorizedException ue) {
                    LOG.error("no user with such credentials is registered", ue);
                    errors.add("no user with such credentials is registered");
                } catch (LoginValidationException | PasswordValidationException e) {
                    LOG.error("error logging in: " + e.getMessage());
                    errors.add(e.getMessage());
                }
            }
            if (!errors.isEmpty()) {
                req.setAttribute("errors", errors);
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            } else {
                try {
                    final String fullToken = authSvc.fullToken(authInfo);
                    final Cookie cookie = new Cookie("auth-info", fullToken);
                    cookie.setMaxAge(14 * 24 * 60 * 60);
                    cookie.setPath("/");
                    resp.addCookie(cookie);
                    final List<INote> notes = contentStorage.get(authInfo);
                    contentSync(req, resp, notes, authInfo.getLogin(), null);
                } catch (final ContentReadException e) {
                    LOG.error(
                        "could not get content for " + authInfo.getLogin()
                            + " " + authInfo.getToken(), e);
                    contentSync(req, resp, Collections.<INote> emptyList(),
                        authInfo.getLogin(),
                        Collections
                            .singletonList("could not load your content"));
                } catch (UnauthorizedException e) {
                    req.setAttribute("errors",
                        Collections.singletonList("authorization error"));
                    req.getRequestDispatcher("/login.jsp").forward(req, resp);
                }
            }
        }
    }

    private static boolean validCaptcha(KaptchaEngine kaptchaEngine,
            String captcha, HttpServletRequest req) {
        return Objects.equals(captcha,
            kaptchaEngine.getGeneratedKeyDestroying(req));
    }

    private static String sanitize(String s) {
        s = s.replaceAll("&", "&amp;");
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        return s;
    }

    protected void contentAsync(final HttpServletRequest req,
            final HttpServletResponse resp, final AuthInfo auth,
            final List<INote> notes, final INote urlNote)
            throws ServletException {
        try {
            LOG.debug("going async");
            final AsyncContext ctx = req.startAsync(req, resp);
            ctx.setTimeout(20000);
            ctx.addListener(new AsyncListener() {
                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    try {
                        HttpServletRequest req =
                            (HttpServletRequest) event.getSuppliedRequest();
                        req.setAttribute("login", auth.getLogin());
                        req.setAttribute("notes", convert(notes));
                        req.setAttribute("errors", Collections
                            .<String> singletonList("timeout fetching url"));
                    } finally {
                        LOG.debug("async finishing due to timeout");
                        event.getAsyncContext().dispatch("/content.jsp");
                    }
                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {
                    // no-op
                }

                @Override
                public void onError(AsyncEvent event) throws IOException {
                    // no-op
                }

                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                    // no-op
                }
            });
            URL url = new URL(LinkUtil.urlize(urlNote.asText()));
            ListenableFuture<LinkInfo> linkInfoFuture =
                LinkParser.parseLinkInfo(url);
            Environment.getPublished(Futurizer.class).addCallback(linkInfoFuture,
                new FutureCallback<LinkInfo>() {
                    @Override
                    public void onSuccess(LinkInfo linkInfo) {
                        List<LinkInfo> allLinkInfos = new ArrayList<>();
                        boolean added = false;
                        try {
                            final ServletRequest req = ctx.getRequest();
                            req.setAttribute("login", auth.getLogin());
                            allLinkInfos.addAll(convert(notes));
                            if (linkInfo != null) {
                                ILink link;
                                if (linkInfo.title != null) {
                                    link =
                                        contentStorage.makeLink(
                                            urlNote.getCoord(), linkInfo.title,
                                            linkInfo.faviconUrl);
                                } else {
                                    link =
                                        contentStorage.makeLink(
                                            urlNote.getCoord(),
                                            linkInfo.content,
                                            linkInfo.faviconUrl);
                                }
                                allLinkInfos.addAll(convert(Collections
                                    .<INote> singletonList(link)));
                            }
                            added = true;
                        } catch (ContentWriteException | ContentReadException
                                | ContentTooLargeException | RuntimeException e) {
                            LOG.error("rendering error", e);
                            req.setAttribute("errors",
                                Collections.singletonList(e.getMessage()));
                        } finally {
                            LOG.debug("async finishing");
                            if (!added) {
                                allLinkInfos.addAll(convert(Collections
                                    .<INote> singletonList(urlNote)));
                            }
                            req.setAttribute("notes", allLinkInfos);
                            ctx.dispatch("/content.jsp");
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        try {
                            LOG.error("parse link infos error", t);
                            final ServletRequest req = ctx.getRequest();
                            req.setAttribute("login", auth.getLogin());
                            req.setAttribute("notes", convert(ImmutableList
                                .<INote> builder().addAll(notes).add(urlNote)
                                .build()));
                            req.setAttribute("errors",
                                Collections.singletonList(t.getMessage()));
                        } finally {
                            LOG.debug("async finishing");
                            ctx.dispatch("/content.jsp");
                        }
                    }
                });
        } catch (MalformedURLException e) {
            try {
                LOG.error("supplied note is not a URL: " + urlNote.asText(), e);
                contentSync(req, resp, notes, auth.getLogin(),
                    Collections.singletonList("internal error"));
            } catch (IOException ioExc) {
                LOG.error("could not inform about malformed url", ioExc);
            }
        }
    }

    static List<LinkInfo> convert(List<INote> notes) {
        return Lists.transform(notes, new Function<INote, LinkInfo>() {
            @Override
            @Nullable
            public LinkInfo apply(@Nullable INote note) {
                try {
                    if (note instanceof ILink) {
                        ILink link = (ILink) note;
                        URL url = new URL(link.asText());
                        String server = LinkParser.getServer(url);
                        String favicon = link.getFaviconUrl();
                        return new LinkInfo(link.getCoord().getId(), true, link
                            .asText(), link.getTitle(), server, favicon);
                    } else {
                        return new LinkInfo(note.getCoord().getId(), false,
                            note.asText(), null, null, null);
                    }
                } catch (MalformedURLException e) {
                    return new LinkInfo(note.getCoord().getId(), false, note
                        .asText(), null, null, null);
                }
            }
        });
    }

    protected void contentSync(final HttpServletRequest req,
            final HttpServletResponse resp, List<INote> notes,
            final String login, List<String> errors) throws ServletException,
            IOException {
        try {
            req.setAttribute("login", login);
            req.setAttribute("notes", convert(notes));
            req.setAttribute("errors", errors);
        } finally {
            LOG.debug("sync finishing");
            req.getRequestDispatcher("/content.jsp").forward(req, resp);
        }
    }
}
