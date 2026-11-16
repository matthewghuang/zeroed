package com.zeroed.app.services;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zeroed.app.dto.CreateExpenseRequest;
import com.zeroed.app.dto.ExpenseResponse;
import com.zeroed.app.exceptions.ExpenseRequestException;
import com.zeroed.app.models.Expense;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.ExpenseRepository;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;

@Service
public class ExpenseService {
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

        Long payerId = request.paidByUserId() == null ? creator.getId() : request.paidByUserId();
        Map<String, String> errors = new LinkedHashMap<>();
        if (!members.containsKey(payerId)) {
            errors.put("paidByUserId", "must reference a current member of the group");
        }
        if (!request.participantIds().stream().allMatch(members::containsKey)) {
            errors.put("participantIds", "must contain only current group members");
        }
        if (!errors.isEmpty()) throw new ExpenseRequestException("validation_failed", errors);

        LocalDate date = request.date() == null ? LocalDate.now(ZoneOffset.UTC) : request.date();
        Expense expense = new Expense(group, request.description(), request.amountMinor(), request.currency(),
                members.get(payerId), creator, date);
        int count = request.participantIds().size();
        long share = request.amountMinor() / count;
        long remainder = request.amountMinor() % count;
        // The request parser sorts IDs; the lowest IDs receive one extra minor unit each.
        for (int index = 0; index < count; index++) {
            expense.addSplit(members.get(request.participantIds().get(index)), share + (index < remainder ? 1 : 0));
        }
        return ExpenseResponse.from(expenses.saveAndFlush(expense));
    }
}
