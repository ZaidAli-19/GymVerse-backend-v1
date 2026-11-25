package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for Expense entities.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    List<Expense> findByExpenseDateBetween(LocalDate start, LocalDate end);

    @Query("""
            SELECT e.category, COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.expenseDate BETWEEN :start AND :end
            GROUP BY e.category
            """)
    List<Object[]> sumAmountGroupedByCategoryBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
