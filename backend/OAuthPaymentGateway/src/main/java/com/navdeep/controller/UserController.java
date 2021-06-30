package com.navdeep.controller;

import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.navdeep.config.CurrentUser;
import com.navdeep.dto.ApiResponse;
import com.navdeep.dto.LocalUser;
import com.navdeep.dto.PasswordDto;
import com.navdeep.model.PasswordResetToken;
import com.navdeep.model.User;
import com.navdeep.service.UserService;
import com.navdeep.util.GeneralUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author nav
 *
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class UserController {

	@Autowired
	UserService userService;
	
	@Autowired
    private JavaMailSender mailSender;
	
	@Autowired
    private Environment env;

	@GetMapping("/user/me")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getCurrentUser(@CurrentUser LocalUser user) {
		return ResponseEntity.ok(GeneralUtils.buildUserInfo(user));
	}

	@GetMapping("/all")
	public ResponseEntity<?> getContent() {
		return ResponseEntity.ok("Everyone can see this");
	}

	@GetMapping("/user")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getUserContent() {
		return ResponseEntity.ok("The person having role user can see this");
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAdminContent() {
		return ResponseEntity.ok("Admin can see this");
	}

	@GetMapping("/mod")
	@PreAuthorize("hasRole('MODERATOR')")
	public ResponseEntity<?> getModeratorContent() {
		return ResponseEntity.ok("Moderator content goes here");
	}

	@PostMapping("/user/resetPassword")
	public ResponseEntity<?> resetPassword(HttpServletRequest request, @RequestParam("email") String userEmail) {
		
		LocalUser localUser = (LocalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		User user = localUser.getUser();		
		if (user == null) {
			return new ResponseEntity<>(new ApiResponse(false, "User not found"), HttpStatus.BAD_REQUEST);
		}
		String password_reset_token = UUID.randomUUID().toString();
		userService.createPasswordResetTokenForUser(user, password_reset_token);

		try {
			final String appUrl = "http://" + request.getServerName() + ":" + request.getServerPort()
					+ request.getContextPath();
			
			//System.out.println("************"+appUrl); localhost:8080
			
			final SimpleMailMessage email = constructResetTokenEmail(appUrl, request.getLocale(), password_reset_token,
					user);
			mailSender.send(email);
		} catch (final MailAuthenticationException e) {
			log.debug("MailAuthenticationException error ", e);
			e.printStackTrace();
			return new ResponseEntity<>(new ApiResponse(false, e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
		} catch (final Exception e) {
			e.printStackTrace();
			log.debug(e.getLocalizedMessage(), e);
			return new ResponseEntity<>(new ApiResponse(false, e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
		}

		return ResponseEntity.ok().body(new ApiResponse(true, "Password reset link sent to your mail" + request.getServerName() + 
				request.getServerPort()));
	}
	
	@GetMapping("/user/checkOldPassToken")
	public ResponseEntity<?> showChangePasswordPage(@RequestParam("token") String token) {
	    String result = userService.validatePasswordResetToken(token);
	    if(result != null) {
	    	return new ResponseEntity<>(new ApiResponse(false, result), HttpStatus.BAD_REQUEST);
	    }
	    return ResponseEntity.ok().body(new ApiResponse(true, "Token found"));
	}
	
	@PostMapping("/user/saveNewPassword")
	public ResponseEntity<?> savePassword(@Valid @RequestBody PasswordDto passwordDto, BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			bindingResult.getFieldErrors().stream().forEach(error -> log.debug(error.getDefaultMessage()));
		}
	    String result = userService.validatePasswordResetToken(passwordDto.getToken());
	    
	    if(result != null) {
	    	return new ResponseEntity<>(new ApiResponse(false, result), HttpStatus.BAD_REQUEST);
	    }
	    
	    PasswordResetToken token = userService.findByToken(passwordDto.getToken());
        User user = token.getUser();
                
	    if(user != null) {
	        userService.changeUserPassword(user, passwordDto.getNewPassword());
	        return ResponseEntity.ok().body(new ApiResponse(true, "Password Reset successful"));
	    }
	    return new ResponseEntity<>(new ApiResponse(false, "Invalid User"), HttpStatus.BAD_REQUEST);
	}

	private SimpleMailMessage constructResetTokenEmail(String contextPath, Locale locale, String token, User user) {
		String url = contextPath + "/user/changePassword?token=" + token;
		String message = "Password click the below link to reset your password";
		return constructEmail("Reset Password", message + " \r\n" + url, user);
	}

	private SimpleMailMessage constructEmail(String subject, String body, User user) {
		SimpleMailMessage email = new SimpleMailMessage();
		email.setSubject(subject);
		email.setText(body);
		email.setTo(user.getEmail());
		email.setFrom(env.getProperty("support.email"));
		return email;
	}
	
	
}