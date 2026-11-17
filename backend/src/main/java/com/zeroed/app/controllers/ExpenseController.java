package com.zeroed.app.controllers;

import java.net.URI;
import java.util.Map;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.zeroed.app.dto.CreateExpenseRequest;
import com.zeroed.app.dto.ExpenseResponse;
import com.zeroed.app.dto.UpdateExpenseRequest;
import com.zeroed.app.exceptions.ExpenseRequestException;
import com.zeroed.app.dto.GroupBalanceResponse;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.UserRepository;
import com.zeroed.app.services.BalanceService;
import com.zeroed.app.services.ExpenseService;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api")
public class ExpenseController {
    private final ExpenseService expenses;
    private final BalanceService balances;
    private final UserRepository users;
    public ExpenseController(ExpenseService expenses, BalanceService balances, UserRepository users) {
        this.expenses = expenses;
        this.balances = balances;
        this.users = users;
    }

    @PostMapping("/groups/{groupId}/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(@PathVariable Long groupId, @RequestBody JsonNode body,
                                                         Authentication authentication) {
        var creator = authenticatedUser(authentication);
        if (creator == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ExpenseResponse expense = expenses.createExpense(groupId, CreateExpenseRequest.from(body), creator);
        return ResponseEntity.created(URI.create("/api/expenses/" + expense.id())).body(expense);
    }
    
    @GetMapping("/groups/{groupId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> listExpenses(@PathVariable Long groupId,
                                                              Authentication authentication) {
        var requester = authenticatedUser(authentication);
        if (requester == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(expenses.listExpenses(groupId, requester));
    }

    @GetMapping("/groups/{groupId}/balances")
    public ResponseEntity<GroupBalanceResponse> getBalances(@PathVariable Long groupId,
                                                             Authentication authentication) {
        var requester = authenticatedUser(authentication);
        if (requester == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(balances.getBalances(groupId, requester));
    }

    @GetMapping("/expenses/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable Long expenseId,
                                                       Authentication authentication) {
        var requester = authenticatedUser(authentication);
        if (requester == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(expenses.getExpense(expenseId, requester));
    }

    @PatchMapping("/expenses/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long expenseId, @RequestBody JsonNode body,
                                                          Authentication authentication) {
        var requester = authenticatedUser(authentication);
        if (requester == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(expenses.updateExpense(expenseId, UpdateExpenseRequest.from(body), requester));
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId, Authentication authentication) {
        var requester = authenticatedUser(authentication);
        if (requester == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        expenses.deleteExpense(expenseId, requester);
        return ResponseEntity.noContent().build();
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return users.findByEmail(authentication.getName()).orElse(null);
    }

    public record RequestError(String error, String message, Map<String, String> fields) {}

    @ExceptionHandler(ExpenseRequestException.class)
    public ResponseEntity<RequestError> invalidRequest(ExpenseRequestException exception) {
        return ResponseEntity.badRequest().body(new RequestError(
                exception.getError(), exception.getMessage(), exception.getFields()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RequestError> malformedJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new RequestError("validation_failed", "Request body is invalid.",
                Map.of("body", "must be valid JSON")));
    }
}
