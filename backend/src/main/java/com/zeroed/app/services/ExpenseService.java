package com.zeroed.app.services;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zeroed.app.dto.CreateExpenseRequest;
import com.zeroed.app.dto.ExpenseResponse;
import com.zeroed.app.dto.UpdateExpenseRequest;
import com.zeroed.app.exceptions.ExpenseRequestException;
import com.zeroed.app.models.Expense;
import com.zeroed.app.models.ExpenseSplit;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.ExpenseRepository;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;

@Service
public class ExpenseService {
    private static final Sort NEWEST_FIRST = Sort.by(
          Sort.Order.desc("date"),
          Sort.Order.desc("createdAt"),
          Sort.Order.desc("id"));
    private final ExpenseRepository expenses;
    private final GroupRepository groups;
    private final GroupMembershipRepository memberships;

    public ExpenseService(ExpenseRepository expenses, GroupRepository groups,
                          GroupMembershipRepository memberships) {
        this.expenses = expenses;
        this.groups = groups;
        this.memberships = memberships;
    }

    @Transactional
    public ExpenseResponse createExpense(Long groupId, CreateExpenseRequest request, User creator) {
        // Unlike member listing, expense creation gives admins no membership bypass.
        if (!memberships.existsById(new GroupMembershipId(creator.getId(), groupId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
        var group = groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        Map<Long, User> members = new HashMap<>();
        for (User user : memberships.findMembersByGroupId(groupId)) members.put(user.getId(), user);
        // Omission means every current member; explicit selections are already sorted by the parser.
        List<Long> participantIds = request.participantIds() == null
                ? members.keySet().stream().sorted().toList()
                : request.participantIds();

        Long payerId = request.paidByUserId() == null ? creator.getId() : request.paidByUserId();
        Map<String, String> errors = new LinkedHashMap<>();
        if (!members.containsKey(payerId)) {
            errors.put("paidByUserId", "must reference a current member of the group");
        }
        if (!participantIds.stream().allMatch(members::containsKey)) {
            errors.put("participantIds", "must contain only current group members");
        }
        if (!errors.isEmpty()) throw new ExpenseRequestException("validation_failed", errors);

        LocalDate date = request.date() == null ? LocalDate.now(ZoneOffset.UTC) : request.date();
        Expense expense = new Expense(group, request.description(), request.amountMinor(), request.currency(),
                members.get(payerId), creator, date);
        int count = participantIds.size();
        long share = request.amountMinor() / count;
        long remainder = request.amountMinor() % count;
        // Both resolved paths are sorted; the lowest IDs receive one extra minor unit each.
        for (int index = 0; index < count; index++) {
            expense.addSplit(members.get(participantIds.get(index)), share + (index < remainder ? 1 : 0));
        }
        return ExpenseResponse.from(expenses.saveAndFlush(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(Long groupId, User requester) {
        if (!memberships.existsById(new GroupMembershipId(requester.getId(), groupId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
        return expenses.findByGroupId(groupId, NEWEST_FIRST).stream()
              .map(ExpenseResponse::from)
              .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(Long expenseId, User requester) {
        Expense expense = expenses.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        requireMembership(expense, requester);
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, UpdateExpenseRequest request, User requester) {
        Expense expense = findForMutation(expenseId, requester);
        Map<Long, User> members;
        if (request.paidByUserId() != null || request.participantIds() != null) {
            members = new HashMap<>();
            for (User user : memberships.findMembersByGroupId(expense.getGroup().getId())) {
                members.put(user.getId(), user);
            }
            Map<String, String> errors = new LinkedHashMap<>();
            if (request.paidByUserId() != null && !members.containsKey(request.paidByUserId())) {
                errors.put("paidByUserId", "must reference a current member of the group");
            }
            if (request.participantIds() != null
                    && !request.participantIds().stream().allMatch(members::containsKey)) {
                errors.put("participantIds", "must contain only current group members");
            }
            if (!errors.isEmpty()) throw new ExpenseRequestException("validation_failed", errors);
        } else {
            members = Map.of();
        }

        Currency currency = request.currency() == null
                ? Currency.getInstance(expense.getCurrency()) : request.currency();
        long amountMinor = request.amountMinor(expense.getAmountMinor(), expense.getCurrencyExponent(), currency);
        User payer = request.paidByUserId() == null ? expense.getPaidBy() : members.get(request.paidByUserId());
        expense.update(request.description() == null ? expense.getDescription() : request.description(),
                amountMinor, currency, payer, request.date() == null ? expense.getDate() : request.date());

        if (request.amount() != null || request.currency() != null || request.participantIds() != null) {
            // Omitted participants remain valid historical entries even after leaving the group.
            List<User> participants = request.participantIds() == null
                    ? expense.getSplits().stream().map(ExpenseSplit::getUser)
                            .sorted(Comparator.comparing(User::getId)).toList()
                    : request.participantIds().stream().map(members::get).toList();
            expense.recalculateEqualSplits(participants);
        }
        expenses.flush();
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void deleteExpense(Long expenseId, User requester) {
        Expense expense = findForMutation(expenseId, requester);
        expenses.delete(expense);
        expenses.flush();
    }

    private Expense findForMutation(Long expenseId, User requester) {
        // PATCH and DELETE serialize on the same parent row before accessing its splits.
        Expense expense = expenses.findByIdForUpdate(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        requireMembership(expense, requester);
        if (!expense.getCreatedBy().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the expense creator may modify it");
        }
        return expense;
    }

    private void requireMembership(Expense expense, User requester) {
        if (!memberships.existsById(new GroupMembershipId(requester.getId(), expense.getGroup().getId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found");
        }
    }
}
