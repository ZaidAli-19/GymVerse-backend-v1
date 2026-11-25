package com.company.GymVerse.tenantAdmin.request.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Standard expense categories presented at the top of the Add Expense workflow.
 */
public enum ExpenseCategory {
    ELECTRICITY,
    STAFF_SALARY,
    RENT,
    MACHINE_REPAIR,
    WATER_BILL,
    MAINTENANCE,
    OTHER;

    public static List<String> getDefaultCategories() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
