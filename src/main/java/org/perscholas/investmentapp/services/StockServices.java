package org.perscholas.investmentapp.services;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.PossessionRepoI;
import org.perscholas.investmentapp.dao.StockRepoI;
import org.perscholas.investmentapp.dto.StockDTO;
import org.perscholas.investmentapp.models.Possession;
import org.perscholas.investmentapp.models.Stock;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
public class StockServices {
    StockRepoI stockRepoI;
    PossessionRepoI possessionRepoI;

    public StockServices(StockRepoI stockRepoI, PossessionRepoI possessionRepoI) {
        this.stockRepoI = stockRepoI;
        this.possessionRepoI = possessionRepoI;
    }

    // updates or creates a stock
    public Stock createOrUpdate(Stock stock) {
        var stockOptional =
                stockRepoI.findByTicker(stock.getTicker());
        if (stockOptional.isPresent()) {
            log.warn("createOrUpdate(): stock with ticker " + stock.getTicker() +
                    " already exists, updating");
            Stock originalStock = stockOptional.get();
            originalStock.setStockName(stock.getStockName());
            originalStock.setPrice(stock.getPrice());
            originalStock.setDescription(stock.getDescription());

            return stockRepoI.save(originalStock);
        } else {
            log.warn("createOrUpdate(): stock with ticker " + stock.getTicker() +
                    " does not exists, adding");
            return stockRepoI.save(stock);
        }
    }

    // ****** CLEANER VERSION
    public Stock savePositionToStock(Integer stockId, Integer possessionId) throws Exception {

        var stockOpt = stockRepoI.findById(stockId);
        var possessionOpt = possessionRepoI.findById(possessionId);
    
        if (stockOpt.isPresent() && possessionOpt.isPresent()) {
            Stock stock = stockOpt.get();
            Possession possession = possessionOpt.get();
    
            log.warn("savePositionToStock(): stock with ticker " + stock.getTicker() +
                    " updating new possession");
    
            stock.addPossession(possession);
    
            return stockRepoI.saveAndFlush(stock);
        } else {
            throw new Exception("saving a possession to the stock with ID " + stockId + " did not go well!!!!!");
        }
    }  

    public List<StockDTO> allStocks() {
        return stockRepoI.findAll()
                .stream()
                .map((stock) -> new StockDTO(stock.getStockName(), stock.getTicker(), stock.getPrice(), stock.getDescription()))
                .collect(Collectors.toList());
    }

    public List<Stock> allRegularStocks() {
        return stockRepoI.findAll()
                .stream()
                .collect(Collectors.toList());
    }
}
