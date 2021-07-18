package com.navdeep.service;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import com.navdeep.dto.LocalUser;
import com.navdeep.dto.SignUpRequest;
import com.navdeep.exception.UserAlreadyExistAuthenticationException;
import com.navdeep.model.PasswordResetToken;
import com.navdeep.model.User;

/**
 * @author nav
 *
 */
public interface UserService {

	public User registerNewUser(SignUpRequest signUpRequest) throws UserAlreadyExistAuthenticationException;

	User findUserByEmail(String email);

	Optional<User> findUserById(Long id);

	LocalUser processUserRegistration(String registrationId, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo);
	
	void createPasswordResetTokenForUser(User user, String token);

	public String validatePasswordResetToken(String token);

	public void changeUserPassword(User user, String password);

	public PasswordResetToken findByToken(String token);

	public String logout(HttpServletRequest request, HttpServletResponse response);
}
