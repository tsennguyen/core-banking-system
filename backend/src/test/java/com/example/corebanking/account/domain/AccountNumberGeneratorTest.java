package com.example.corebanking.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AccountNumberGeneratorTest {

    @Mock private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("nextAccountNumber generates valid 12-digit Luhn account number")
    void nextAccountNumber_shouldGenerate12DigitValidLuhn() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        AccountNumberGenerator generator = new AccountNumberGenerator(jdbcTemplate);
        String accountNumber = generator.nextAccountNumber();

        assertThat(accountNumber).hasSize(12);
        assertThat(accountNumber).startsWith("10000000001");
        assertThat(AccountNumberGenerator.isValidLuhn(accountNumber)).isTrue();
    }

    @Test
    @DisplayName("calculateLuhnCheckDigit computes accurate check digit")
    void calculateLuhnCheckDigit_shouldProduceAccurateDigit() {
        // Test with known payload "10000000001"
        String payload = "10000000001";
        int checkDigit = AccountNumberGenerator.calculateLuhnCheckDigit(payload);
        String fullNumber = payload + checkDigit;

        assertThat(AccountNumberGenerator.isValidLuhn(fullNumber)).isTrue();
    }

    @Test
    @DisplayName("isValidLuhn rejects corrupted or wrong length account numbers")
    void isValidLuhn_shouldRejectInvalidInputs() {
        assertThat(AccountNumberGenerator.isValidLuhn(null)).isFalse();
        assertThat(AccountNumberGenerator.isValidLuhn("")).isFalse();
        assertThat(AccountNumberGenerator.isValidLuhn("100")).isFalse();
        assertThat(AccountNumberGenerator.isValidLuhn("100000000019"))
                .isFalse(); // Incorrect check digit
        assertThat(AccountNumberGenerator.isValidLuhn("10000000001X")).isFalse(); // Non-digit
    }
}
