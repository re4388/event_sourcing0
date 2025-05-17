package com.ben.es0.api;

import com.ben.es0.command.CreateAccountCommand;
import com.ben.es0.command.DepositCommand;
import com.ben.es0.command.WithdrawCommand;
import com.ben.es0.commandhandling.AccountCommandHandler;
import com.ben.es0.domain.events.AccountEvent;
import com.ben.es0.query.AccountQueryService;
import com.ben.es0.query.AccountReadModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountCommandHandler commandHandler;
    private final AccountQueryService queryService;

    @Autowired
    public AccountController(AccountCommandHandler commandHandler, AccountQueryService queryService) {
        this.commandHandler = commandHandler;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<String> createAccount(@RequestBody CreateAccountCommand command) {
        try {
            commandHandler.handle(command);
            return ResponseEntity.status(HttpStatus.CREATED).body("Account created successfully: " + command.getAccountId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating account: " + e.getMessage());
        }
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<String> depositMoney(@PathVariable String accountId, @RequestBody DepositCommand command) {
        if (!accountId.equals(command.getAccountId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Account ID in path and body do not match.");
        }
        try {
            commandHandler.handle(command);
            return ResponseEntity.ok("Deposit successful for account: " + accountId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // Account not found
        } catch (RuntimeException e) {
            // Catch concurrency conflict or other errors
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during deposit: " + e.getMessage());
        }
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<String> withdrawMoney(@PathVariable String accountId, @RequestBody WithdrawCommand command) {
        if (!accountId.equals(command.getAccountId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Account ID in path and body do not match.");
        }
        try {
            commandHandler.handle(command);
            return ResponseEntity.ok("Withdrawal successful for account: " + accountId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // Account not found
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); // Insufficient funds
        } catch (RuntimeException e) {
            // Catch concurrency conflict or other errors
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during withdrawal: " + e.getMessage());
        }
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountState(@PathVariable String accountId) {
        Optional<AccountReadModel> accountState = queryService.getAccountState(accountId);
        if (accountState.isPresent()) {
            return ResponseEntity.ok(accountState.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found: " + accountId);
        }
    }

    @GetMapping("/{accountId}/events")
    public ResponseEntity<?> getAccountEvents(@PathVariable String accountId) {
        List<AccountEvent> events = queryService.getEventsForAccount(accountId);
        if (!events.isEmpty()) {
            return ResponseEntity.ok(events);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No events found for account: " + accountId);
        }
    }
}
