package com.loanmust.loan.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgencyRepository extends JpaRepository<Agency, Long> {
    List<Agency> findByDeletedFalseOrderByNameAsc();

    /** The agency the public website belongs to (the application runs one agency). */
    Optional<Agency> findFirstByDeletedFalseOrderByIdAsc();
}
