package com.pratik.financegpt.repository;

import com.pratik.financegpt.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio , Long> {
    List<Portfolio> findByUsername(String username);
    List<Portfolio> findByUsernameAndSymbol(String username , String symbol);
}
