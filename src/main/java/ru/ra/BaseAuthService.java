package ru.ra;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import ru.ra.Environment.IDisposable;
import ru.ra.errors.LoginValidationException;
import ru.ra.errors.PasswordValidationException;
import ru.ra.errors.UnauthorizedException;

public class BaseAuthService implements IAuthService, IDisposable {
    // don't change. ever, unless want to invalidate all users's data at once
    private String tokenHashSalt;

    // change to invalidate all user's web auth
    private String loginAndTokenSalt;

    private char[] password;

    public BaseAuthService(String tokenHashSalt, String loginAndTokenSalt, String password) {
        this.tokenHashSalt = tokenHashSalt;
        this.loginAndTokenSalt = loginAndTokenSalt;
        this.password = password.toCharArray();
    }

    protected String computeToken(String login, String password)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String id = login + "_" + tokenHashSalt + "_" + password;
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(id.getBytes("utf-8"));
        byte[] hash64 = Base64.encodeBase64(hash);
        return new String(hash64, "utf-8");
    }

    @Override
    public AuthInfo login(final String login, String password)
            throws UnauthorizedException, LoginValidationException, PasswordValidationException {
        try {
            if (!valid(login)) {
                throw new LoginValidationException(
                        "login can only contain english letters, digits, '-' and '_' symbols");
            }
            if (login.length() > 64) {
                throw new LoginValidationException(
                        "login cannot be longer than 64 symbols");
            }
            if (password.length() > 64) {
                throw new LoginValidationException(
                        "password cannot be longer than 64 symbols");
            }
            final String token = computeToken(login, password);
            return new AuthInfo() {
                @Override
                public String getToken() {
                    return token;
                }

                @Override
                public String getLogin() {
                    return login;
                }
            };
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new UnauthorizedException(e);
        }
    }

    private boolean valid(String login) {
        for (char c : login.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') {
                return false;
            }
        }
        return true;
    }

    @Override
    public String fullToken(AuthInfo auth) throws UnauthorizedException {
        String inner = auth.getLogin() + "|" + loginAndTokenSalt + "|" + auth.getToken();
        try {
            String encrypted = encrypt(inner);
            return auth.getLogin() + "|" + encrypted;
        } catch (GeneralSecurityException | IOException e) {
            throw new UnauthorizedException();
        }
    }

    @Override
    public AuthInfo loginByToken(String fullToken) throws UnauthorizedException {
        if (StringUtils.isEmpty(fullToken)) {
            throw new UnauthorizedException();
        }
        if (fullToken.length() > 1024) {
            throw new UnauthorizedException();
        }
        String[] parts = fullToken.split("\\|");
        if (parts.length < 2) {
            throw new UnauthorizedException();
        }
        final String login = parts[0];
        final String encrypted = parts[1];
        try {
            String decrypted = decrypt(encrypted);
            parts = decrypted.split("\\|");
            if (parts.length < 3) {
                throw new UnauthorizedException();
            }
            String login2 = parts[0];
            if (!StringUtils.equals(login, login2)) {
                throw new UnauthorizedException();
            }
            if (!StringUtils.equals(parts[1], loginAndTokenSalt)) {
                throw new UnauthorizedException();
            }
            final String token = parts[2];
            return new AuthInfo() {
                @Override
                public String getLogin() {
                    return login;
                }

                @Override
                public String getToken() {
                    return token;
                }
            };
        } catch (GeneralSecurityException | IOException e) {
            throw new UnauthorizedException(e);
        }
    }

    @Override
    public void dispose() {
    }

    public byte[] decrypt(final byte[] data) throws GeneralSecurityException {
        return getCipher(Cipher.DECRYPT_MODE).doFinal(data);
    }

    public String decrypt(final String data) throws GeneralSecurityException, IOException {
        return new String(decrypt(Base64.decodeBase64(URLDecoder.decode(data, "UTF-8").getBytes("UTF-8"))), "UTF-8");
    }

    public byte[] encrypt(final byte[] data) throws GeneralSecurityException {
        return getCipher(Cipher.ENCRYPT_MODE).doFinal(data);
    }

    public String encrypt(final String data) throws GeneralSecurityException, IOException {
        return URLEncoder.encode(new String(Base64.encodeBase64(encrypt(data.getBytes("UTF-8"))), "UTF-8"), "UTF-8");
    }

    private final byte[] salt = {
            (byte) 0xAA, (byte) 0x9B, (byte) 0x8C, (byte) 0x32,
            (byte) 0x65, (byte) 0x53, (byte) 0x3E, (byte) 0x03};

    private Cipher getCipher(final int mode) throws GeneralSecurityException {
        final int iterationCount = 19;
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterationCount);
        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        Cipher result = Cipher.getInstance(key.getAlgorithm());
        result.init(mode, key, new PBEParameterSpec(salt, iterationCount));
        return result;
    }
}
