package org.perscholas.investmentapp.services;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.*;

import org.perscholas.investmentapp.models.*;


import org.perscholas.investmentapp.security.AppUserDetailService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
public class UserServices {
    private final StockRepoI stockRepoI;
    private final AuthGroupRepoI authGroupRepoI;
    UserRepoI userRepoI;
    PossessionRepoI possessionRepoI;
    AddressRepoI addressRepoI;
    AppUserDetailService appUserDetailService;


    @Autowired
    public UserServices(UserRepoI userRepoI, PossessionRepoI possessionRepoI,
                        AuthGroupRepoI authGroupRepoI, AddressRepoI addressRepoI,
                        AppUserDetailService appUserDetailService,
                        StockRepoI stockRepoI) {
        this.userRepoI = userRepoI;
        this.possessionRepoI = possessionRepoI;
        this.authGroupRepoI = authGroupRepoI;
        this.addressRepoI = addressRepoI;
        this.appUserDetailService = appUserDetailService;

        this.stockRepoI = stockRepoI;
    }

    public User createOrUpdate(User user) throws Exception {
        if (user == null) 
        throw new Exception("createOrUpdate(): user was null");
        if (user.getEmail() == null || user.getEmail().isBlank())
            throw new Exception("createOrUpdate(): email was null/blank");

        Optional<User> userOptional = userRepoI.findByEmailAllIgnoreCase(user.getEmail());

        if (userOptional.isPresent()) {
            log.warn("createOrUpdate(): user with email " + user.getEmail() +
                    " already exists");
            User originalUser = userOptional.get();

            originalUser.setFirstName(user.getFirstName());
            originalUser.setLastName(user.getLastName());
            originalUser.setEmail(user.getEmail());

            log.warn("createOrUpdate(): user with email " + user.getEmail() +
                    " is updated");

            return userRepoI.save(originalUser);
        } 

        log.debug("createOrUpdate(): user with email " + user.getEmail() + " has been created");

        AuthGroup newAuth = new AuthGroup(user.getEmail(), "ROLE_USER");
        authGroupRepoI.save(newAuth);

        return userRepoI.save(user);
    }

    public User createOrUpdate(User user, User editUser) {
        Optional<User> userOptional = userRepoI.findByEmailAllIgnoreCase(user.getEmail());

        if (userOptional.isPresent() || user.getId() != null) {
            log.debug("createOrUpdate(): user with email " + user.getEmail() + " already exists");
            User originalUser = userOptional.get();

            originalUser.setFirstName(editUser.getFirstName());
            originalUser.setLastName(editUser.getLastName());
            originalUser.setEmail(editUser.getEmail());

            log.debug("createOrUpdate(): user with email " + user.getEmail() + " already exists");

            return userRepoI.save(originalUser);
        } else {
            log.debug("createOrUpdate(): user with email " + user.getEmail() + " has been created");

            AuthGroup newAuth = new AuthGroup(user.getEmail(), "ROLE_USER");
            authGroupRepoI.save(newAuth);

            return userRepoI.save(user);
        }
    }

    // probably just have to make this one a create only, for /signup
    public User createOrUpdateRunning(User user) {
        Optional<User> userOptional = userRepoI.findByEmailAllIgnoreCase(user.getEmail());

        if (userOptional.isPresent()) {
            log.debug("UserServices: user with email " + user.getEmail() + " already exists");
            User originalUser = userOptional.get();
            originalUser.setFirstName(user.getFirstName());
            originalUser.setLastName(user.getLastName());

            return userRepoI.save(originalUser);
        } else {
            log.debug("UserServices: user with email " + user.getEmail() + " has been created");

            return userRepoI.save(user);
        }
    }

    public User addOrUpdateAddress(Address address, User user) {
        Optional<Address> addressOptional = addressRepoI.findById(address.getId());
        if (addressOptional.isPresent()) {
            Address originalAddress = addressOptional.get();
            originalAddress.setStreet(address.getStreet());
            originalAddress.setState(address.getState());
            originalAddress.setZipcode(address.getZipcode());

            addressRepoI.save(originalAddress);
            return user;
        } else {
            addressRepoI.save(address);
            user.setAddress(address);

            userRepoI.save(user);

            return user;
        }
    }



    // used to delete on User Account (within UserController)
    public User deletePossesionToUser(String userEmail, Integer possessionId) throws Exception {

        Optional<User> optionalUser = userRepoI.findByEmailAllIgnoreCase(userEmail);
        Optional<Possession> userPossession = possessionRepoI.findById(possessionId);
    
        if (optionalUser.isPresent() && userPossession.isPresent()) {
    
            User confirmedUser = optionalUser.get();
            Possession confirmedPossession = userPossession.get();
    
            // Ownership check (prevents deleting someone else's possession)
            if (confirmedPossession.getUser() == null
                    || confirmedPossession.getUser().getId() == null
                    || !confirmedPossession.getUser().getId().equals(confirmedUser.getId())) {
                throw new Exception("deletePossesionToUser failed: possessionId=" + possessionId
                        + " does not belong to user=" + userEmail);
            }
    
            var confirmedStock = confirmedPossession.getStock();
    
            confirmedUser.removePossession(confirmedPossession);
            if (confirmedStock != null) {
                confirmedStock.removePossession(confirmedPossession);
                stockRepoI.saveAndFlush(confirmedStock);
            }
    
            userRepoI.saveAndFlush(confirmedUser);
            possessionRepoI.delete(confirmedPossession);
    
            return confirmedUser;
    
        } else {
            throw new Exception("deletePossesionToUser failed: possessionId=" + possessionId + ", email=" + userEmail);
        }
    }

    
    public User deletePossessionByTicker(String userEmail, String ticker) throws Exception {

        var optionalUser = userRepoI.findByEmailAllIgnoreCase(userEmail);
        var optionalStock = stockRepoI.findByTicker(ticker);
    
        if (optionalUser.isPresent() && optionalStock.isPresent()) {
            User confirmedUser = optionalUser.get();
            Stock confirmedStock = optionalStock.get();
    
            // find possession by confirmed entities
            var userPossession = possessionRepoI.findByUserAndStock(confirmedUser, confirmedStock);
    
            if (userPossession.isEmpty()) {
                throw new Exception("deletePossessionByTicker failed: no possession for user=" + userEmail + ", ticker=" + ticker);
            }
    
            Possession confirmedPossession = userPossession.get();
    
            confirmedUser.removePossession(confirmedPossession);
            confirmedStock.removePossession(confirmedPossession);
    
            userRepoI.saveAndFlush(confirmedUser);
            stockRepoI.saveAndFlush(confirmedStock);
            possessionRepoI.delete(confirmedPossession);
    
            return confirmedUser;
        }
    
        throw new Exception("deletePossessionByTicker failed: user=" + userEmail + ", ticker=" + ticker);
    }
    
    public List<Possession> retrievePortfolio(String email) throws Exception {

        if (email == null || email.isBlank()) {
            throw new Exception("retrievePortfolio: email was null/blank - cannot retrieve portfolio!!!!!");
        }
    
        if (userRepoI.findByEmailAllIgnoreCase(email).isPresent()) {
            log.debug("retrievePortfolio: user exists, retrieving portfolio for " + email);
    
            List<Possession> portfolio = possessionRepoI.findByUserEmailWithStock(email);
    
            log.debug("retrievePortfolio: retrievePortfolio was successful, positions found = " + portfolio.size());
            return portfolio;
    
        } else {
            throw new Exception("retrievePortfolio: user " + email + " does not exist - portfolio retrieval did not go well!!!!!");
        }
    }
    

}
