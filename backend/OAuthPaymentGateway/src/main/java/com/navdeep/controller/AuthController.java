package com.navdeep.controller;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.navdeep.config.CurrentUser;
import com.navdeep.dto.ApiResponse;
import com.navdeep.dto.JwtAuthenticationResponse;
import com.navdeep.dto.LocalUser;
import com.navdeep.dto.LoginRequest;
import com.navdeep.dto.SignUpRequest;
import com.navdeep.dto.SignUpResponse;
import com.navdeep.exception.UserAlreadyExistAuthenticationException;
import com.navdeep.model.User;
import com.navdeep.security.jwt.TokenProvider;
import com.navdeep.service.UserService;
import com.navdeep.util.CaptchaUtil;
import com.navdeep.util.CookieUtils;
import com.navdeep.util.GeneralUtils;

import cn.apiclub.captcha.Captcha;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import lombok.extern.slf4j.Slf4j;

/**
 * @author nav
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserService userService;

	@Autowired
	TokenProvider tokenProvider;

	@Autowired
	private QrDataFactory qrDataFactory;

	@Autowired
	private QrGenerator qrGenerator;

	@Autowired
	private CodeVerifier verifier;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		LocalUser localUser = (LocalUser) authentication.getPrincipal();
		boolean authenticated = !localUser.getUser().isUsing2FA();
		String jwt = tokenProvider.createToken(localUser, authenticated);

		HttpHeaders responseHeaders = new HttpHeaders();
		addAccessTokenCookie(responseHeaders, jwt, 864000000l); // Need to remove hard-coded duration

		return ResponseEntity.ok().headers(responseHeaders).body(new JwtAuthenticationResponse(jwt, authenticated,
				authenticated ? GeneralUtils.buildUserInfo(localUser) : null));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
		try {
			/*
			 * if (!signUpRequest.getCaptcha().equals(signUpRequest.getHiddenCaptcha())) {
			 * log.error("Invalid Captcha"); return new ResponseEntity<>(new
			 * ApiResponse(false, "Invalid Captcha!"), HttpStatus.BAD_REQUEST); }
			 */
			User user = userService.registerNewUser(signUpRequest);
			if (signUpRequest.isUsing2FA()) {
				QrData data = qrDataFactory.newBuilder().label(user.getEmail()).secret(user.getSecret())
						.issuer("navdeep").build();

				// Generate the QR code image data as a base64 string which is used in image tag
				// in HTML front-end

				String qrCodeImage = getDataUriForImage(qrGenerator.generate(data), qrGenerator.getImageMimeType());
				return ResponseEntity.ok().body(new SignUpResponse(true, qrCodeImage));
			}
		} catch (UserAlreadyExistAuthenticationException e) {
			log.error("Exception Ocurred", e);
			return new ResponseEntity<>(new ApiResponse(false, "Email Address already in use!"),
					HttpStatus.BAD_REQUEST);
		} catch (QrGenerationException e) {
			log.error("QR Generation Exception Ocurred", e);
			return new ResponseEntity<>(new ApiResponse(false, "Unable to generate QR code!"), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok().body(new ApiResponse(true, "User registered successfully"));
	}

	/**
	 * This method is used to verify the 6 digit code for 2FA
	 * 
	 * @param code
	 * @param user
	 * @return
	 */
	@PostMapping("/verify")
	@PreAuthorize("hasRole('PRE_VERIFICATION_USER')")
	public ResponseEntity<?> verifyCode(@NotEmpty @RequestBody String code, @CurrentUser LocalUser user) {
		if (!verifier.isValidCode(user.getUser().getSecret(), code)) {
			return new ResponseEntity<>(new ApiResponse(false, "Invalid Code!"), HttpStatus.BAD_REQUEST);
		}
		String jwt = tokenProvider.createToken(user, true);
		return ResponseEntity.ok(new JwtAuthenticationResponse(jwt, true, GeneralUtils.buildUserInfo(user)));
	}

	@GetMapping("/captcha")
	private ResponseEntity<?> getCaptcha() {
		User user = new User();
		Captcha captcha = CaptchaUtil.createCaptcha(240, 70);
		user.setHiddenCaptcha(captcha.getAnswer());
		user.setCaptcha(""); // value entered by the User
		user.setRealCaptcha(CaptchaUtil.encodeCaptcha(captcha));
		return ResponseEntity.ok(user);

	}

	private void addAccessTokenCookie(HttpHeaders httpHeaders, String token, Long duration) {
		httpHeaders.add(HttpHeaders.SET_COOKIE, CookieUtils.createAccessTokenCookie(token, duration).toString());
	}
	
	@GetMapping("/logout")
    public ResponseEntity<?> logOut(HttpServletRequest request, HttpServletResponse response){		
		return ResponseEntity.ok().body(new ApiResponse(true, userService.logout(request, response)));

    }
}