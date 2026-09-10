package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SeasonExportRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 16, fraction = 2) BigDecimal quantity,
        @NotBlank @Size(max = 30) String unit,
        @NotNull @PastOrPresent LocalDate exportDate,
        @NotBlank @Size(max = 255) String warehouse) {}
