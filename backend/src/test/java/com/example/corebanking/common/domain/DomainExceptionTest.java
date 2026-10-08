package com.example.corebanking.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    @DisplayName("BusinessRuleException with ErrorCode should retain code and status")
    void businessRuleException_withErrorCode_retainsAttributes() {
        BusinessRuleException ex =
                new BusinessRuleException(ErrorCode.LIMIT_EXCEEDED, "Daily limit exceeded");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LIMIT_EXCEEDED);
        assertThat(ex.getErrorCode().getHttpStatus()).isEqualTo(422);
        assertThat(ex.getErrorCode().getProblemTypeUri())
                .isEqualTo("urn:problem-type:limit-exceeded");
        assertThat(ex.getMessage()).isEqualTo("Daily limit exceeded");
    }

    @Test
    @DisplayName("BusinessRuleException with String code should resolve known error code")
    void businessRuleException_withStringCode_resolvesKnownCode() {
        BusinessRuleException ex =
                new BusinessRuleException("INSUFFICIENT_BALANCE", "Balance is too low");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        assertThat(ex.getErrorCode().getHttpStatus()).isEqualTo(422);
    }

    @Test
    @DisplayName("ResourceNotFoundException should format message and error code 404")
    void resourceNotFoundException_withResourceAndId_formatsProperly() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Customer", 42L);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(ex.getErrorCode().getHttpStatus()).isEqualTo(404);
        assertThat(ex.getMessage()).contains("Customer").contains("42");
    }

    @Test
    @DisplayName("DuplicateResourceException should have error code 409")
    void duplicateResourceException_hasConflictStatus() {
        DuplicateResourceException ex =
                new DuplicateResourceException("Account number already exists");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(ex.getErrorCode().getHttpStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("InvalidStatusTransitionException should have error code 409")
    void invalidStatusTransitionException_hasConflictStatus() {
        InvalidStatusTransitionException ex =
                new InvalidStatusTransitionException("Cannot activate closed account");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(ex.getErrorCode().getHttpStatus()).isEqualTo(409);
    }
}
