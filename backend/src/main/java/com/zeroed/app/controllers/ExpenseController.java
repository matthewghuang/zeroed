package com.zeroed.app.controllers;

import java.net.URI;
import java.util.Map;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.zeroed.app.dto.CreateExpenseRequest;
import com.zeroed.app.dto.ExpenseResponse;
import com.zeroed.app.exceptions.ExpenseRequestException;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.UserRepository;
import com.zeroed.app.services.ExpenseService;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {
    private final ExpenseService expenses;
    private final UserRepository users;

    public ExpenseController(ExpenseService expenses, UserRepository users) {
        this.expenses = expenses;
        this.users = users;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@PathVariable Long groupId, @RequestBody JsonNode body,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var creator = users.findByEmail(authentication.getName()).orElse(null);
        if (creator == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ExpenseResponse expense = expenses.createExpense(groupId, CreateExpenseRequest.from(body), creator);
        return ResponseEntity.created(URI.create("/api/expenses/" + expense.id())).body(expense);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> listExpenses(@PathVariable Long groupId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var requester = users.findByEmail(authentication.getName()).orElse(null);
        if (requester == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(expenses.listExpenses(groupId, requester));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable Long groupId,
            @PathVariable Long expenseId,
            Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = users.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ExpenseResponse> expenseList = expenses.listExpenses(groupId, currentUser);

        for (ExpenseResponse exp : expenseList) {
            if (exp.id() == expenseId) {
                return ResponseEntity.ok(exp);
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    public record RequestError(String error, String message, Map<String, String> fields) {
    }

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
