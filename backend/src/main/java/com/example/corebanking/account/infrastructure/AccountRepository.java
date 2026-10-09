package com.example.corebanking.account.infrastructure;

import com.example.corebanking.account.domain.Account;
import com.example.corebanking.account.domain.AccountStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for Account entity with Specification support. */
@Repository
public interface AccountRepository
        extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByCustomerIdAndStatusNot(Long customerId, AccountStatus status);

    Optional<Account> findByAccountNumber(String accountNumber);
}
