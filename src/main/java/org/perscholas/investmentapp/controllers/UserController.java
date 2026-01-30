package org.perscholas.investmentapp.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.*;
import org.perscholas.investmentapp.dto.StockDTO;
import org.perscholas.investmentapp.models.*;
import org.perscholas.investmentapp.services.PossessionServices;
import org.perscholas.investmentapp.services.StockServices;
import org.perscholas.investmentapp.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@Slf4j
@SessionAttributes(value = {"currentUser"})
@RequestMapping("/user")
class UserController {

    private final AuthGroupRepoI authGroupRepoI;
    private final AddressRepoI addressRepoI;
    private final UserRepoI userRepoI;
    private final StockRepoI stockRepoI;
    private final PossessionRepoI possessionRepoI;

    private final UserServices userServices;
    private final StockServices stockServices;
    private final PossessionServices possessionServices;

    @Autowired
    public UserController(AddressRepoI addressRepoI, UserRepoI userRepoI,
                          StockRepoI stockRepoI, PossessionRepoI possessionRepoI,
                          UserServices userServices, StockServices stockServices,
                          PossessionServices possessionServices,
                          AuthGroupRepoI authGroupRepoI) {
        this.addressRepoI = addressRepoI;
        this.userRepoI = userRepoI;
        this.stockRepoI = stockRepoI;
        this.possessionRepoI = possessionRepoI;
        this.userServices = userServices;
        this.stockServices = stockServices;
        this.possessionServices = possessionServices;
        this.authGroupRepoI = authGroupRepoI;
    }

    // Helper: always prefer User email for DB writes (prevents session/currentUser drift)
    private String requireUserEmail(HttpServletRequest request) throws Exception {
        Principal p = request.getUserPrincipal();
        if (p == null) throw new Exception("Not authenticated (principal is null)");
        return p.getName();
    }

    @GetMapping("/dashboard")
    public String getUserWithID(@ModelAttribute("currentUser") User user,
                                Model model,
                                HttpSession http) throws Exception {

        log.warn("the value of CurrentUser is " + user);
        log.warn("the attr of session currentUser in model is " + http.getAttribute("currentUser"));

        if (user != null) {
            List<StockDTO> allStocks = stockServices.allStocks();
            model.addAttribute("allStocks", allStocks);

            List<Possession> userPortfolio = userServices.retrievePortfolio(user.getEmail());
            model.addAttribute("userPortfolio", userPortfolio);
        } else {
            throw new Exception("/user/dashboard: currentUser was null");
        }

        return "userdashboard";
    }

    @PostMapping("/dashboard/addstock")
    public String addStock(@ModelAttribute("currentUser") User user,
                           @RequestParam("ticker") String ticker,
                           @RequestParam("shares") double shares,
                           HttpServletRequest request) throws Exception {

        log.warn("/user/dashboard/addstock: add stock has initialized");
        log.warn("/user/dashboard/addstock: " + user);
        log.warn("/user/dashboard/addstock: " + (user != null ? user.getEmail() : "null"));

        // ✅ FIX: don't use stockRepoI.findByTicker(ticker).get()
        // ✅ FIX: don't build Possession using detached session User/Stock
        String userEmail = requireUserEmail(request);
        possessionServices.addOrUpdatePosition(userEmail, ticker, shares);

        log.warn("user/dashboard/addstock: stock has been added/updated for user " + userEmail);
        return "redirect:/user/dashboard";
    }

    @GetMapping("/portfolio")
    public String portfolio(@ModelAttribute("currentUser") User user,
                            Model model,
                            HttpSession http) throws Exception {

        log.warn("portfolio(): currentUser from @ModelAttribute = {}", (user != null ? user.getEmail() : "null"));


        if (user != null) {
            List<Possession> userPortfolio = userServices.retrievePortfolio(user.getEmail());
            model.addAttribute("userPortfolio", userPortfolio);
        } else {
            throw new Exception("/user/portfolio: currentUser was null");
        }

        return "userportfolio";
    }

    // form is done on modal
    @PostMapping("/portfolio/edit")
    public String editPossession(@ModelAttribute("currentUser") User user,
                                 @RequestParam("ticker") String ticker,
                                 @RequestParam("shares") double shares,
                                 HttpServletRequest request) throws Exception {

        log.warn("/user/portfolio/edit: edit possession has initialized");
        log.warn("/user/portfolio/edit: " + user);
        log.warn("/user/portfolio/edit: " + (user != null ? user.getEmail() : "null"));

        // ✅ FIX: same as addStock—use principal email + ticker + shares, let service confirm entities
        String userEmail = requireUserEmail(request);
        possessionServices.addOrUpdatePosition(userEmail, ticker, shares);

        log.warn("user/portfolio/edit: possession has been edited for " + userEmail);
        return "redirect:/user/portfolio";
    }

    /**
     * ✅ Keep your existing endpoint for backwards compatibility:
     * POST /user/portfolio/delete/{ticker}
     *
     * But route it through a safe DB-confirmed delete (no detached entities).
     */
    @PostMapping("/portfolio/delete/{ticker}")
    public String deletePossessionByTicker(@ModelAttribute("currentUser") User user,
                                           @PathVariable(name = "ticker") String ticker,
                                           HttpServletRequest request) throws Exception {

        if (ticker != null) {
            log.warn("/user/portfolio/delete/{ticker}: delete possession has initialized");
            log.warn("/user/portfolio/delete/{ticker}: ticker=" + ticker);
            log.warn("/user/portfolio/delete/{ticker}: " + user);
            log.warn("/user/portfolio/delete/{ticker}: " + (user != null ? user.getEmail() : "null"));

            String userEmail = requireUserEmail(request);

            // ✅ Safe: delete by (userEmail + ticker) using DB-confirmed entities
            // You can implement this method in UserServices OR PossessionServices.
            userServices.deletePossessionByTicker(userEmail, ticker);

            log.warn("user/portfolio/delete/{ticker}: possession has been removed from user " + userEmail);
            return "redirect:/user/portfolio";
        } else {
            throw new Exception("/user/portfolio/delete/{ticker}: ticker was null");
        }
    }

    /**
     * ✅ Recommended new endpoint (more robust than ticker):
     * POST /user/portfolio/delete with possessionId.
     * Update Thymeleaf to use this when you can.
     */
    @PostMapping("/portfolio/delete")
    public String deletePossessionById(HttpServletRequest request,
                                       @RequestParam("possessionId") Integer possessionId) throws Exception {

        String userEmail = requireUserEmail(request);

        log.warn("/user/portfolio/delete: delete possession has initialized");
        log.warn("/user/portfolio/delete: possessionId=" + possessionId);
        log.warn("/user/portfolio/delete: userEmail=" + userEmail);

        userServices.deletePossesionToUser(userEmail, possessionId);

        log.warn("user/portfolio/delete: possession has been removed from user " + userEmail);
        return "redirect:/user/portfolio";
    }

    @GetMapping("/account")
    public String account(@ModelAttribute("currentUser") User user,
                          Model model) throws Exception {

        if (user != null) {
            log.warn("/user/account: CurrentUser is not null, email is " + user.getEmail());

            User editUser = new User();
            editUser.setFirstName(user.getFirstName());
            editUser.setLastName(user.getLastName());
            editUser.setEmail(user.getEmail());
            model.addAttribute("editUser", editUser);

            // ✅ guard address access
            if (user.getAddress() != null) {
                Address editAddress = addressRepoI.findById(user.getAddress().getId())
                        .orElseThrow(() -> new Exception("/user/account: address not found for id=" + user.getAddress().getId()));
                model.addAttribute("editAddress", editAddress);
            } else {
                model.addAttribute("editAddress", new Address());
            }

        } else {
            throw new Exception("/user/account: currentUser is not logged in");
        }

        return "useraccount";
    }

    @PostMapping("/account/edit")
    public String editAccount(@ModelAttribute("currentUser") User user,
                              @Valid @ModelAttribute("editUser") User editUser,
                              BindingResult bindingResult,
                              HttpServletRequest request) throws Exception {

        log.warn("/user/account/edit: editUser is not null, info is " + editUser);

        if (bindingResult.hasErrors()) {
            log.debug(bindingResult.getAllErrors().toString());
            return "useraccount";
        }

        Principal p = request.getUserPrincipal();
        if (editUser != null && p != null) {

            User principalUser = userRepoI.findByEmail(p.getName())
                    .orElseThrow(() -> new Exception("/user/account/edit: principal user not found: " + p.getName()));

            log.warn("/user/account/edit: CurrentUser(principal) is not null, email is " + principalUser.getEmail());

            List<AuthGroup> authGroupList = authGroupRepoI.findByEmail(principalUser.getEmail());
            if (authGroupList.isEmpty()) {
                throw new Exception("/user/account/edit: auth groups not found for: " + principalUser.getEmail());
            }

            AuthGroup userAuth = authGroupList.get(0);
            userAuth.setEmail(editUser.getEmail());
            authGroupRepoI.saveAndFlush(userAuth);

            editUser = userServices.createOrUpdate(principalUser, editUser);

            // ✅ FIX: you had user.setEmail(editUser.getLastName()) — that breaks session identity
            user.setFirstName(editUser.getFirstName());
            user.setLastName(editUser.getLastName());
            user.setEmail(editUser.getEmail());

            log.warn("/user/account/edit: User " + editUser.getEmail() + " was updated");
            log.warn("/user/account/edit: Session user " + user.getEmail() + " was updated");

        } else {
            throw new Exception("/user/account/edit: currentUser is not logged in");
        }

        return "redirect:/logout";
    }

    @PostMapping("/account/edit_address")
    public String editAddress(@ModelAttribute("currentUser") User user,
                              @Valid @ModelAttribute("editAddress") Address editAddress,
                              BindingResult bindingResult) throws Exception {

        if (bindingResult.hasErrors()) {
            log.debug(bindingResult.getAllErrors().toString());
            return "useraccount";
        }

        if (editAddress != null && user != null) {
            log.warn("/user/account/edit_address: editAddress is not null, info is " + editAddress + " id: " + editAddress.getId());
            userServices.addOrUpdateAddress(editAddress, user);
        } else {
            throw new Exception("/user/account/edit_address: editAddress was null");
        }

        return "redirect:/user/account";
    }
}

/* BEFORE FIX OF ADDING BACK ALL METHODS, DELETING AND 'ACCOUNT' SITE DO NOT WORK 
package org.perscholas.investmentapp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.UserRepoI;
import org.perscholas.investmentapp.dto.StockDTO;
import org.perscholas.investmentapp.models.Possession;
import org.perscholas.investmentapp.models.User;
import org.perscholas.investmentapp.services.PossessionServices;
import org.perscholas.investmentapp.services.StockServices;
import org.perscholas.investmentapp.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@Slf4j
@RequestMapping("/user")
class UserController {

    private final UserRepoI userRepoI;
    private final UserServices userServices;
    private final StockServices stockServices;
    private final PossessionServices possessionServices;

    @Autowired
    public UserController(UserRepoI userRepoI,
                          UserServices userServices,
                          StockServices stockServices,
                          PossessionServices possessionServices) {
        this.userRepoI = userRepoI;
        this.userServices = userServices;
        this.stockServices = stockServices;
        this.possessionServices = possessionServices;
    }

    private User requireUser(Principal principal) throws Exception {
        if (principal == null) throw new Exception("Not authenticated");
        return userRepoI.findByEmail(principal.getName())
                .orElseThrow(() -> new Exception("Authenticated user not found in DB: " + principal.getName()));
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) throws Exception {
        User user = requireUser(principal);
        log.warn("dashboard(): principal user = {}", user.getEmail());

        List<StockDTO> allStocks = stockServices.allStocks();
        model.addAttribute("allStocks", allStocks);

        List<Possession> userPortfolio = userServices.retrievePortfolio(user.getEmail());
        model.addAttribute("userPortfolio", userPortfolio);

        return "userdashboard";
    }

    @PostMapping("/dashboard/addstock")
    public String addStock(Principal principal,
                           @RequestParam("ticker") String ticker,
                           @RequestParam("shares") double shares) throws Exception {

        User user = requireUser(principal);
        log.warn("addStock(): user={}, ticker={}, shares={}", user.getEmail(), ticker, shares);

        possessionServices.addOrUpdatePosition(user.getEmail(), ticker, shares);

        return "redirect:/user/dashboard";
    }

    @GetMapping("/portfolio")
    public String portfolio(Principal principal, Model model) throws Exception {
        User user = requireUser(principal);

        List<Possession> userPortfolio = userServices.retrievePortfolio(user.getEmail());
        model.addAttribute("userPortfolio", userPortfolio);

        return "userportfolio";
    }

    // ✅ Safe delete (sell/remove) — only pass primitives, service reloads confirmed entities
    @PostMapping("/portfolio/delete")
    public String deletePossession(Principal principal,
                                   @RequestParam("possessionId") Integer possessionId) throws Exception {
    
        User user = requireUser(principal);
        log.warn("deletePossession(): user={}, possessionId={}", user.getEmail(), possessionId);
    
        // Call UserServices (since delete is in UserServices in your codebase)
        userServices.deletePossesionToUser(user.getEmail(), possessionId);
    
        return "redirect:/user/portfolio";
    }

    // Account page (simple GET)
    @GetMapping("/account")
    public String account(Principal principal, Model model) throws Exception {
        User user = requireUser(principal);
        model.addAttribute("user", user);
        return "account";
    }
}
*/