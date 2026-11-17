package com.zeroed.app.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.zeroed.app.models.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupId(Long groupId, Sort sort);
    @Query("""
            select distinct expense from Expense expense
            join fetch expense.paidBy
            left join fetch expense.splits split
            left join fetch split.user
            where expense.group.id = :groupId
            """)
    List<Expense> findForBalancesByGroupId(@Param("groupId") Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select expense from Expense expense where expense.id = :expenseId")
    Optional<Expense> findByIdForUpdate(@Param("expenseId") Long expenseId);
}
