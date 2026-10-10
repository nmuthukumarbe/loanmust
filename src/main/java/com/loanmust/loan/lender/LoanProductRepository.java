package com.loanmust.loan.lender;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    Optional<LoanProduct> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<LoanProduct> findByTenantIdAndLenderIdAndDeletedFalseOrderByLoanTypeAscNameAsc(Long tenantId, Long lenderId);

    List<LoanProduct> findByTenantIdAndDeletedFalseOrderByLoanTypeAscRateMinAsc(Long tenantId);

    List<LoanProduct> findByTenantIdAndLoanTypeAndDeletedFalseOrderByRateMinAsc(Long tenantId, LoanType loanType);

    List<LoanProduct> findByTenantIdAndLoanTypeAndActiveTrueAndDeletedFalseOrderByRateMinAsc(Long tenantId, LoanType loanType);

    List<LoanProduct> findByTenantIdAndActiveTrueAndDeletedFalseOrderByLenderNameAscNameAsc(Long tenantId);

    long countByTenantIdAndLenderIdAndDeletedFalse(Long tenantId, Long lenderId);
}
