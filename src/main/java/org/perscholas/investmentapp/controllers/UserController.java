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
@SessionAttributes(value = {"currentUser"})   // NOTE: keeping this since your flow relies on it
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

    // Helper: always prefer principal email for DB writes (prevents session/currentUser drift)
    private String requireUserEmail(HttpServletRequest request) throws Exception {
        Principal p = request.getUserPrincipal();
        if (p == null) throw new Exception("Not authenticated (principal is null)");
        return p.getName();
    }

    @GetMapping("/dashboard")
    public String getUserWithID(@ModelAttribute("currentUser") User user,
                                Model model,
                                HttpSession http) throws Exception {

        log.warn("dashboard(): currentUser model attr = {}", user);
        log.warn("dashboard(): session currentUser attr = {}", http.getAttribute("currentUser"));

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
        log.warn("/user/dashboard/addstock: currentUser={}", user);
        log.warn("/user/dashboard/addstock: currentUserEmail={}", (user != null ? user.getEmail() : "null"));

        // ✅ FIX: Don't do stockRepoI.findByTicker(ticker).get()
        // ✅ FIX: Don't build Possession with detached session entities
        String userEmail = requireUserEmail(request);
        possessionServices.addOrUpdatePosition(userEmail, ticker, shares);

        log.warn("/user/dashboard/addstock: stock added/updated for user {}", userEmail);
        return "redirect:/user/dashboard";
    }

    @GetMapping("/portfolio")
    public String portfolio(@ModelAttribute("currentUser") User user,
                            Model model,
                            HttpSession http) throws Exception {

        log.warn("/user/portfolio: session currentUser attr = {}", http.getAttribute("currentUser"));
        log.warn("/user/portfolio: currentUser model attr email = {}", (user != null ? user.getEmail() : "null"));

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
        log.warn("/user/portfolio/edit: currentUser={}", user);
        log.warn("/user/portfolio/edit: currentUserEmail={}", (user != null ? user.getEmail() : "null"));

        // ✅ FIX: same safe flow as addStock
        String userEmail = requireUserEmail(request);
        possessionServices.addOrUpdatePosition(userEmail, ticker, shares);

        log.warn("/user/portfolio/edit: possession updated for {}", userEmail);
        return "redirect:/user/portfolio";
    }

    /**
     * ✅ KEEP: your existing endpoint so the website still works:
     * POST /user/portfolio/delete/{ticker}
     */
    @PostMapping("/portfolio/delete/{ticker}")
    public String deletePossession(@ModelAttribute("currentUser") User user,
                                   @PathVariable(name = "ticker") String ticker,
                                   HttpServletRequest request) throws Exception {

        if (ticker != null) {
            log.warn("/user/portfolio/delete/{ticker}: delete possession has initialized");
            log.warn("/user/portfolio/delete/{ticker}: ticker={}", ticker);
            log.warn("/user/portfolio/delete/{ticker}: currentUser={}", user);
            log.warn("/user/portfolio/delete/{ticker}: currentUserEmail={}", (user != null ? user.getEmail() : "null"));

            String userEmail = requireUserEmail(request);

            // ✅ FIX: delete using DB-confirmed user + stock, not detached session entities
            userServices.deletePossessionByTicker(userEmail, ticker);

            log.warn("/user/portfolio/delete/{ticker}: possession removed from {}", userEmail);
            return "redirect:/user/portfolio";
        } else {
            throw new Exception("/user/portfolio/delete/{ticker}: ticker was null");
        }
    }

    @GetMapping("/account")
    public String account(@ModelAttribute("currentUser") User user,
                          Model model) throws Exception {

        if (user != null) {
            log.warn("/user/account: CurrentUser is not null, email is {}", user.getEmail());

            User editUser = new User();
            editUser.setFirstName(user.getFirstName());
            editUser.setLastName(user.getLastName());
            editUser.setEmail(user.getEmail());
            model.addAttribute("editUser", editUser);

            // ✅ FIX: don't assume address exists
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

        Principal p = request.getUserPrincipal();
        log.warn("/user/account/edit: editUser = {}", editUser);

        if (bindingResult.hasErrors()) {
            log.debug(bindingResult.getAllErrors().toString());
            return "useraccount";
        }

        if (editUser != null && p != null) {

            User principalUser = userRepoI.findByEmail(p.getName())
                    .orElseThrow(() -> new Exception("/user/account/edit: principal user not found: " + p.getName()));

            log.warn("/user/account/edit: principal user email = {}", principalUser.getEmail());

            List<AuthGroup> authGroupList = authGroupRepoI.findByEmail(principalUser.getEmail());
            if (authGroupList.isEmpty()) {
                throw new Exception("/user/account/edit: auth group not found for " + principalUser.getEmail());
            }

            AuthGroup userAuth = authGroupList.get(0);
            userAuth.setEmail(editUser.getEmail());
            authGroupRepoI.saveAndFlush(userAuth);

            editUser = userServices.createOrUpdate(principalUser, editUser);

            // ✅ FIX: keep session user in sync (you had editUser.getLastName() before)
            user.setFirstName(editUser.getFirstName());
            user.setLastName(editUser.getLastName());
            user.setEmail(editUser.getEmail());

            log.warn("/user/account/edit: User {} was updated", editUser.getEmail());
            log.warn("/user/account/edit: Session user now {}", user.getEmail());

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
            log.warn("/user/account/edit_address: editAddress is not null, info is {} id: {}", editAddress, editAddress.getId());
            userServices.addOrUpdateAddress(editAddress, user);
        } else {
            throw new Exception("/user/account/edit_address: editAddress was null");
        }

        return "redirect:/user/account";
    }
}
