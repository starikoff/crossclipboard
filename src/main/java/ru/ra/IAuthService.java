package ru.ra;

import ru.ra.errors.LoginValidationException;
import ru.ra.errors.PasswordValidationException;
import ru.ra.errors.UnauthorizedException;

public interface IAuthService {
    AuthInfo login(String login, String password) throws UnauthorizedException,
            LoginValidationException, PasswordValidationException;

    AuthInfo loginByToken(String token) throws UnauthorizedException;

    String fullToken(AuthInfo auth) throws UnauthorizedException;
}
