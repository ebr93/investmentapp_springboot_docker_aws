package org.perscholas.investmentapp.services;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.PossessionRepoI;
import org.perscholas.investmentapp.dao.StockRepoI;
import org.perscholas.investmentapp.dao.UserRepoI;
import org.perscholas.investmentapp.models.Possession;
import org.perscholas.investmentapp.models.Stock;
import org.perscholas.investmentapp.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(rollbackOn = Exception.class)
public class PossessionServices {

    UserRepoI userRepoI;
    PossessionRepoI possessionRepoI;
    StockRepoI stockRepoI;

    @Autowired
    public PossessionServices(UserRepoI userRepoI,
                             PossessionRepoI possessionRepoI,
                             StockRepoI stockRepoI,
                             UserServices userServices,   // kept for constructor compatibility (unused here)
                             StockServices stockServices  // kept for constructor compatibility (unused here)
    ) {
        this.userRepoI = userRepoI;
        this.possessionRepoI = possessionRepoI;
        this.stockRepoI = stockRepoI;
    }

    /**
     * Professional rule: never persist relationships using detached entities from the web layer.
     * Always "confirm" (reload) the User + Stock from the DB first, then save/update using those.
     */
    public Possession createOrUpdate(Possession possession) throws Exception {
        if (possession == null || possession.getUser() == null || possession.getStock() == null) {
            throw new IllegalArgumentException("createOrUpdate(): possession, user, and stock are required.");
        }

        // Confirm entities from DB (managed + valid IDs)
        User confirmedUser = userRepoI.findByEmail(possession.getUser().getEmail())
                .orElseThrow(() -> new Exception("createOrUpdate(): user not found: " + possession.getUser().getEmail()));

        Stock confirmedStock = stockRepoI.findByTicker(possession.getStock().getTicker())
                .orElseThrow(() -> new Exception("createOrUpdate(): stock not found: " + possession.getStock().getTicker()));

        // Find existing position using confirmed entities
        Optional<Possession> existingOpt = possessionRepoI.findByUserAndStock(confirmedUser, confirmedStock);

        if (existingOpt.isPresent()) {
            Possession existing = existingOpt.get();
            existing.setShares(possession.getShares());
            log.debug("createOrUpdate(): updated shares for {} / {}", confirmedUser.getEmail(), confirmedStock.getTicker());
            return possessionRepoI.save(existing);
        }

        // Create new position using confirmed entities
        Possession newPossession = new Possession(possession.getShares(), confirmedUser, confirmedStock);

        // Persist possession
        newPossession = possessionRepoI.saveAndFlush(newPossession);

        // Maintain your current "portfolio" modeling (UserServices.retrievePortfolio reads from user's collection)
        confirmedUser.addPossession(newPossession);
        confirmedStock.addPossession(newPossession);

        userRepoI.save(confirmedUser);
        stockRepoI.save(confirmedStock);

        log.debug("createOrUpdate(): created new possession for {} / {}", confirmedUser.getEmail(), confirmedStock.getTicker());
        return newPossession;
    }

    /**
     * Keep this overload, but make it safe: confirm user/stock from DB and delegate.
     */
    public Possession createOrUpdate(Possession possession, User user, Stock stock) throws Exception {
        if (possession == null || user == null || stock == null) {
            throw new IllegalArgumentException("createOrUpdate(possession,user,stock): all params are required.");
        }

        // Force the possession to use the provided identities (then delegate to the safe path)
        possession.setUser(user);
        possession.setStock(stock);
        return createOrUpdate(possession);
    }

    public Possession addOrUpdatePosition(String userEmail, String ticker, double shares) throws Exception {
        User confirmedUser = userRepoI.findByEmail(userEmail)
                .orElseThrow(() -> new Exception("User not found: " + userEmail));
    
        Stock confirmedStock = stockRepoI.findByTicker(ticker)
                .orElseThrow(() -> new Exception("Stock not found: " + ticker));
    
        Optional<Possession> existing = possessionRepoI.findByUserAndStock(confirmedUser, confirmedStock);
    
        if (existing.isPresent()) {
            Possession p = existing.get();
            p.setShares(shares);
            return possessionRepoI.saveAndFlush(p);
        }
    
        Possession p = new Possession(shares, confirmedUser, confirmedStock);
        return possessionRepoI.saveAndFlush(p);
    }
    
    
}
