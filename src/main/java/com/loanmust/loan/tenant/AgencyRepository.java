package com.loanmust.loan.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgencyRepository extends JpaRepository<Agency, Long> {
    List<Agency> findByDeletedFalseOrderByNameAsc();
}
