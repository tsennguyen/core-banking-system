package com.example.corebanking.account.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Generates unique 12-digit bank account numbers using: Prefix "100" (3 digits) + sequence (8
 * digits) + Luhn check digit (1 digit).
 */
@Component
public class AccountNumberGenerator {

    private static final String SEQUENCE_SQL = "SELECT nextval('core.account_number_seq')";
    private static final String PREFIX = "100";

    private final JdbcTemplate jdbcTemplate;

    public AccountNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Obtains the next sequence value and returns a valid 12-digit account number.
     *
     * @return 12-digit account number satisfying Luhn check digit
     */
    public String nextAccountNumber() {
        Long nextVal = jdbcTemplate.queryForObject(SEQUENCE_SQL, Long.class);
        if (nextVal == null) {
            throw new IllegalStateException(
                    "Failed to obtain nextval from core.account_number_seq");
        }
        String payload = PREFIX + String.format("%08d", nextVal); // 11 digits
        int checkDigit = calculateLuhnCheckDigit(payload);
        return payload + checkDigit;
    }

    /**
     * Calculates the Luhn mod-10 check digit for an 11-digit payload.
     *
     * @param payload 11-digit numeric string
     * @return single check digit (0-9)
     */
    public static int calculateLuhnCheckDigit(String payload) {
        int sum = 0;
        boolean doubleDigit = true; // position 1 from right (the last payload char) is doubled
        for (int i = payload.length() - 1; i >= 0; i--) {
            int digit = payload.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        int mod = sum % 10;
        return (mod == 0) ? 0 : 10 - mod;
    }

    /**
     * Verifies if a given 12-digit string satisfies the Luhn algorithm.
     *
     * @param number 12-digit account number
     * @return true if valid Luhn number, false otherwise
     */
    public static boolean isValidLuhn(String number) {
        if (number == null || number.length() != 12) {
            return false;
        }
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            char c = number.charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
            int digit = c - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
