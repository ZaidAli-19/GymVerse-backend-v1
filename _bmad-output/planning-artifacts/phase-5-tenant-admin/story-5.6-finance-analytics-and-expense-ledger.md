---
id: "story-5.6"
epic_id: "epic-phase-5-tenant-admin"
title: "Finance Analytics Suite & Expense Ledger"
status: "ready-for-review"
type: "gateway-operation"
created_at: "2026-11-25"
dependencies:
  - "story-5.1"
  - "story-5.3"
files_to_modify:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/RevenueRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/ExpenseRepository.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/specifications/finance/ExpenseSpecification.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/finance/FetchFinanceOverviewRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/finance/FetchExpenseTransactionsRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/finance/CreateExpenseRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/finance/FinanceOverviewResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/finance/ExpenseCategoryBreakdownResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/finance/CollectionBreakdownResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/finance/RevenueSourceBreakdownResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/finance/ExpenseTransactionResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/finance/FetchFinanceOverviewService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/finance/FetchExpenseTransactionsService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/finance/CreateExpenseService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/service/finance/FinanceServiceTest.java"
---

# Story 5.6: Finance Analytics Suite & Expense Ledger

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **BigDecimal Precision**: All currency amounts and percentages MUST use `BigDecimal` (`RoundingMode.HALF_UP`, scale 2), never `float` or `double`. Guard divide-by-zero cleanly when `totalRevenue == 0`.
> 4. **Subfolder Classification**: Services in `service/finance/`, Requests in `request/finance/`, Responses in `response/finance/`, Specifications in `specifications/finance/`.

---

## 1. User Story

**As a** Tenant Administrator monitoring my gym's financial health on the Finance page,  
**I want** weekly/monthly/yearly toggles showing total revenue, total expense, net profit, budget-used percentage, category-wise expense breakdowns, cash/online collections, revenue source breakdowns (`NEW_MEMBERSHIP`, `RENEWAL`, `OTHER`), a paginated expense transaction ledger, and an Add Expense form,  
**So that** I can track profitability and operational spending accurately.

---

## 2. Technical Context & Operations

### 2.1 `FETCH_FINANCE_OVERVIEW`
- Request (`FetchFinanceOverviewRequest { periodType (WEEKLY | MONTHLY | YEARLY, default MONTHLY), referenceDate (optional LocalDate, default today) }`).
- `FinancePeriodHelper` computes exact `[startDate, endDate]` and UTC `[startInstant, endInstant]` for the chosen `periodType`.
- Returns `FinanceOverviewResponse`:
  - `periodType`, `startDate`, `endDate`
  - **KPIs & Overview Tab**: `totalRevenue`, `totalExpense`, `netProfit` (`totalRevenue.subtract(totalExpense)`), `budgetUsedPercentage` (`(totalExpense / totalRevenue) * 100`, `0.00` if `totalRevenue == 0`)
  - **Expense Breakdown Tab**: `expenseBreakdown`: list of `ExpenseCategoryBreakdownResponse { category, amount, percentageOfExpense }` (e.g., `ELECTRICITY`, `STAFF_SALARY`, `RENT`, `MACHINE_REPAIR`, `WATER_BILL`, `MAINTENANCE`, `OTHER`, or custom categories)
  - **Collection Tab**: `collectionBreakdown`: `CollectionBreakdownResponse { cashCollected, onlineCollected, totalCollected }`
  - **Revenue Breakdown Tab**: `revenueBreakdown`: `RevenueSourceBreakdownResponse { newMembershipRevenue, renewalRevenue, otherRevenue, totalRevenue }`

### 2.2 `FETCH_EXPENSE_TRANSACTIONS`
- Request (`FetchExpenseTransactionsRequest { periodType (optional), startDate (optional), endDate (optional), category (optional), search (optional), page, size, sortBy, sortDirection }`).
- If `periodType` is supplied without explicit `startDate`/`endDate`, `FinancePeriodHelper` resolves the date range cleanly.
- Specification (`ExpenseSpecification.withFilters(startDate, endDate, category, search)`):
  - `startDate` / `endDate` on `expenseDate`
  - `category` (ignore `"ALL"`, case-insensitive match)
  - `search`: case-insensitive `LIKE` across `title` and `description`
- Returns `PagedResponse<ExpenseTransactionResponse>`.

### 2.3 `CREATE_EXPENSE`
- Request (`CreateExpenseRequest { title, category, customCategory (optional when category == OTHER or free-text), amount, description (note), expenseDate (default today) }`).
- Validation checks `title` non-blank, `category` non-blank (maps standard `ExpenseCategory` defaults `ELECTRICITY`, `STAFF_SALARY`, `RENT`, `MACHINE_REPAIR`, `WATER_BILL`, `MAINTENANCE`, `OTHER` cleanly), `amount > 0`.
- Persists `Expense` linked to `recordedByAdmin` (`TenantAdminContextHelper`) and returns HTTP `201 Created` with `ExpenseTransactionResponse`.

---

## 3. Acceptance Criteria

- [ ] `FETCH_FINANCE_OVERVIEW` computes `WEEKLY`, `MONTHLY`, and `YEARLY` windows accurately using `FinancePeriodHelper` and `BigDecimal` math (with zero-revenue divide guard).
- [ ] Expense breakdown, cash/online collection breakdown, and revenue source breakdown (`NEW_MEMBERSHIP`, `RENEWAL`, `OTHER`) match underlying records.
- [ ] `FETCH_EXPENSE_TRANSACTIONS` uses `ExpenseSpecification`, pagination, and whitelisted sorting (`id`, `expenseDate`, `amount`, `category`, `title`).
- [ ] `CREATE_EXPENSE` supports all default expense categories + notes and returns `201 Created`.
- [ ] Unit tests in `FinanceServiceTest` verify weekly/monthly/yearly aggregation, zero-revenue percentage guard, specification filtering, and expense creation.
