package com.account_service.services.Impl;

import com.account_service.entity.Account;
import com.account_service.entity.Customer;
import com.account_service.exceptions.ResourceNotFoundException;
import com.account_service.repositories.AccountRepository;
import com.account_service.services.AccountService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final RestTemplate restTemplate;
    private final AccountRepository accountRepository;

    private Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

    public AccountServiceImpl(RestTemplate restTemplate,
                              AccountRepository accountRepository) {
        this.restTemplate = restTemplate;
        this.accountRepository = accountRepository;
    }

    @Override
    public Account create(Account account) {
        String accountId = UUID.randomUUID().toString();
        account.setAccountId(accountId);

        Date currentDate = new Date();
        account.setAccountOpeningDate(currentDate);
        account.setLastActivity(currentDate);

        return accountRepository.save(account);
    }

    @Override
    public List<Account> getAccounts() {
        return accountRepository.findAll();
    }

    @Override
    @CircuitBreaker(name = "customerService", fallbackMethod = "fallbackGetAccount")
    public Account getAccount(String id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Customer customer = restTemplate.getForObject(
                "http://customer-service/customer/" + account.getCustomerId(),
                Customer.class
        );

        account.setCustomer(customer);
        return account;
    }

    public Account fallbackGetAccount(String id, Exception ex) {
        logger.error("Customer service is down: {}", ex.getMessage());

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setCustomer(null);
        return account;
    }

    @Override
    public List<Account> getAccountByCustomerId(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    @Override
    public Account updateAccount(String id, Account account) {
        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        existing.setAccountType(account.getAccountType());
        existing.setLastActivity(new Date());

        return accountRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        accountRepository.delete(account);
    }
}
