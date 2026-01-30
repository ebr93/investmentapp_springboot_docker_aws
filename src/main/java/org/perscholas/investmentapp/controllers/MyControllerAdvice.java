package org.perscholas.investmentapp.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.UserRepoI;
import org.perscholas.investmentapp.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;

@ControllerAdvice
@Slf4j
public class MyControllerAdvice {

    private final UserRepoI userRepoI;

    @Autowired
    public MyControllerAdvice(UserRepoI userRepoI) {
        this.userRepoI = userRepoI;
    }

    @ExceptionHandler({Exception.class, AccessDeniedException.class})
    public RedirectView exceptionHandle(Exception ex) {
        log.debug("error happened", ex);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl("http://localhost:8080/error");
        return redirectView;
    }

    // NOTE: Put the authenticated user into the MODEL (not HttpSession).
    // This avoids stale users when you sign up / log in / log out.
    @ModelAttribute
    public void loggedInUser(Model model, HttpServletRequest request) {
        Principal p = request.getUserPrincipal();
        if (p != null) {
            userRepoI.findByEmail(p.getName()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
                log.warn("MyControllerAdvice: currentUser model attr = {}", user.getEmail());
            });
        }
    }
}
