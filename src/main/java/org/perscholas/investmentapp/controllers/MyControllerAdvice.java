package org.perscholas.investmentapp.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.UserRepoI;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;

@ControllerAdvice
@Slf4j
public class MyControllerAdvice {

    private final UserRepoI userRepoI;

    public MyControllerAdvice(UserRepoI userRepoI) {
        this.userRepoI = userRepoI;
    }

    // Only handle access denied here (permission issues)
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied", ex);
        return new ModelAndView("redirect:/403");
    }

    // Put authenticated user into the MODEL each request
    // (works fine with your current approach; avoids stale session user)
    @ModelAttribute
    public void loggedInUser(Model model, HttpServletRequest request) {
        Principal p = request.getUserPrincipal();
        if (p == null) return;

        userRepoI.findByEmail(p.getName()).ifPresent(user -> {
            model.addAttribute("currentUser", user);
            log.warn("MyControllerAdvice: currentUser model attr = {}", user.getEmail());
        });
    }
}
