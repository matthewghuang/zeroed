package com.zeroed.app.services;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Currency;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zeroed.app.dto.GroupBalanceResponse;
import com.zeroed.app.dto.GroupMemberResponse;
import com.zeroed.app.models.Expense;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.ExpenseRepository;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;

@Service
public class BalanceService {
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final Comparator<SettlementEntry> LARGEST_FIRST = Comparator
            .comparing(SettlementEntry::remaining).reversed()
            .thenComparing(entry -> entry.profile().id());

    private final ExpenseRepository expenses;
    private final GroupMembershipRepository memberships;
    private final GroupRepository groups;

    public BalanceService(ExpenseRepository expenses, GroupMembershipRepository memberships, GroupRepository groups) {
        this.expenses = expenses;
        this.memberships = memberships;
        this.groups = groups;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public GroupBalanceResponse getBalances(Long groupId, User requester) {
        if (!memberships.existsById(new GroupMembershipId(requester.getId(), groupId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
        var group = groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        Map<Long, GroupMemberResponse> roster = new TreeMap<>();
        for (User member : memberships.findMembersByGroupId(groupId)) addProfile(roster, member);
        int exponent = Currency.getInstance(group.getBaseCurrency()).getDefaultFractionDigits();
        Map<Long, BigInteger> paidTotals = new TreeMap<>();
        Map<Long, BigInteger> owedTotals = new TreeMap<>();
        for (Expense expense : expenses.findForBalancesByGroupId(groupId)) {
            addProfile(roster, expense.getPaidBy());
            paidTotals.merge(expense.getPaidBy().getId(),
                    BigInteger.valueOf(expense.getConvertedAmountMinor()), BigInteger::add);
            for (var split : expense.getSplits()) {
                addProfile(roster, split.getUser());
                owedTotals.merge(split.getUser().getId(), BigInteger.valueOf(split.getAmountMinor()), BigInteger::add);
            }
        }

        List<GroupMemberResponse> members = List.copyOf(roster.values());
        Map<Long, BigInteger> net = new TreeMap<>();
        List<GroupBalanceResponse.MemberBalance> memberBalances = new ArrayList<>(members.size());
        for (GroupMemberResponse member : members) {
            BigInteger paid = paidTotals.getOrDefault(member.id(), ZERO);
            BigInteger owed = owedTotals.getOrDefault(member.id(), ZERO);
            BigInteger balance = paid.subtract(owed);
            net.put(member.id(), balance);
            memberBalances.add(new GroupBalanceResponse.MemberBalance(member.id(),
                    format(paid, exponent), format(owed, exponent), format(balance, exponent)));
        }
        var sections = List.of(new GroupBalanceResponse.CurrencyBalance(group.getBaseCurrency(), memberBalances,
                transfers(members, net, exponent)));
        return new GroupBalanceResponse(groupId, members, sections, Instant.now());
    }

    private static void addProfile(Map<Long, GroupMemberResponse> roster, User user) {
        roster.putIfAbsent(user.getId(), new GroupMemberResponse(user.getId(), user.getEmail()));
    }

    private static List<GroupBalanceResponse.Transfer> transfers(List<GroupMemberResponse> members,
                                                                  Map<Long, BigInteger> net, int exponent) {
        PriorityQueue<SettlementEntry> creditors = new PriorityQueue<>(LARGEST_FIRST);
        PriorityQueue<SettlementEntry> debtors = new PriorityQueue<>(LARGEST_FIRST);
        for (GroupMemberResponse member : members) {
            BigInteger balance = net.get(member.id());
            if (balance.signum() > 0) creditors.add(new SettlementEntry(member, balance));
            else if (balance.signum() < 0) debtors.add(new SettlementEntry(member, balance.negate()));
        }

        List<GroupBalanceResponse.Transfer> result = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            SettlementEntry creditor = creditors.remove();
            SettlementEntry debtor = debtors.remove();
            BigInteger amount = creditor.remaining().min(debtor.remaining());
            result.add(new GroupBalanceResponse.Transfer(debtor.profile(), creditor.profile(), format(amount, exponent)));
            BigInteger creditorRemainder = creditor.remaining().subtract(amount);
            BigInteger debtorRemainder = debtor.remaining().subtract(amount);
            if (creditorRemainder.signum() != 0) creditors.add(new SettlementEntry(creditor.profile(), creditorRemainder));
            if (debtorRemainder.signum() != 0) debtors.add(new SettlementEntry(debtor.profile(), debtorRemainder));
        }
        return result;
    }

    private static String format(BigInteger minor, int exponent) {
        return new BigDecimal(minor, exponent).toPlainString();
    }

    private record SettlementEntry(GroupMemberResponse profile, BigInteger remaining) {}
}
