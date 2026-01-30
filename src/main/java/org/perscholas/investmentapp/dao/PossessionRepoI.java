package org.perscholas.investmentapp.dao;

import org.perscholas.investmentapp.models.Possession;
import org.perscholas.investmentapp.models.Stock;
import org.perscholas.investmentapp.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface PossessionRepoI extends JpaRepository<Possession,Integer> {
    Optional<Possession> findByUserAndStock(User user, Stock stock);
    Optional<List<Possession>> findByUser(User user);

    Optional<List<Possession>> findByStock(Stock stock);

    @Query("select p from Possession p join fetch p.stock where p.user.email = :email")
    List<Possession> findByUserEmailWithStock(@Param("email") String email);

}
