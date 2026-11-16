package com.zeroed.app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeroed.app.models.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}
