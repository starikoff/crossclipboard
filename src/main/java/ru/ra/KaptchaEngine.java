package ru.ra;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.util.Config;

public class KaptchaEngine {

    private Producer kaptchaProducer = null;

    private static final Map<String, String> captchas =
        new ConcurrentHashMap<String, String>();

    public KaptchaEngine() {
        ImageIO.setUseCache(false);

        Properties props = new Properties();
        props.put("kaptcha.border", "no");
        props.put("kaptcha.textproducer.font.color", "black");
        props.put("kaptcha.textproducer.char.space", "5");
        Config config = new Config(props);

        this.kaptchaProducer = config.getProducerImpl();
    }

    public void captcha(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String id = req.getParameter("id");

        String capText = this.kaptchaProducer.createText();
        captchas.put(id, capText);

        resp.setHeader("Cache-Control", "no-store, no-cache");
        resp.setContentType("image/jpeg");
        BufferedImage bi = this.kaptchaProducer.createImage(capText);
        ServletOutputStream out = resp.getOutputStream();
        ImageIO.write(bi, "jpg", out);
    }

    public String getGeneratedKeyDestroying(HttpServletRequest req) {
        String captchaId = req.getParameter("captchaId");
        return captchaId == null ? null : captchas.remove(captchaId);
    }
}