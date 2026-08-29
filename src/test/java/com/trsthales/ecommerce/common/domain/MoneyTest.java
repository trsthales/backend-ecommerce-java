package com.trsthales.ecommerce.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money Value Object Tests")
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Nested
    @DisplayName("Arithmetic Operations & Immutability")
    class ArithmeticOperations {

        @Test
        @DisplayName("Should perform addition and maintain 2 decimal places")
        void shouldAddMoney() {
            Money m1 = Money.of("10.50");
            Money m2 = Money.of("5.25");

            Money result = m1.plus(m2);

            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("15.75"));
            assertThat(result.currency()).isEqualTo(Money.BRL);
            // Immutability check
            assertThat(m1.amount()).isEqualByComparingTo(new BigDecimal("10.50"));
        }

        @Test
        @DisplayName("Should perform subtraction")
        void shouldSubtractMoney() {
            Money m1 = Money.of("10.50");
            Money m2 = Money.of("5.25");

            Money result = m1.minus(m2);

            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("5.25"));
        }

        @Test
        @DisplayName("Should multiply by integer quantity")
        void shouldMultiplyByQuantity() {
            Money m1 = Money.of("19.99");

            Money result = m1.multiply(3);

            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("59.97"));
        }

        @Test
        @DisplayName("Should multiply by BigDecimal factor with half-even rounding")
        void shouldMultiplyByFactor() {
            Money m1 = Money.of("100.00");

            Money result = m1.multiply(new BigDecimal("0.155"));

            // 100 * 0.155 = 15.50
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("15.50"));
        }

        @Test
        @DisplayName("Should reject arithmetic between different currencies")
        void shouldRejectDifferentCurrencies() {
            Money brl = Money.of("10.00");
            Money usd = Money.of(new BigDecimal("10.00"), USD);

            assertThatThrownBy(() -> brl.plus(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Moedas incompatíveis");

            assertThatThrownBy(() -> brl.minus(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Moedas incompatíveis");

            assertThatThrownBy(() -> brl.compareTo(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Moedas incompatíveis");
        }
    }

    @Nested
    @DisplayName("Comparisons & Predicates")
    class ComparisonsAndPredicates {

        @Test
        @DisplayName("Should correctly evaluate positive, zero and comparisons")
        void shouldEvaluatePredicates() {
            Money pos = Money.of("10.00");
            Money zero = Money.ZERO;
            Money neg = Money.of("-5.00");

            assertThat(pos.isPositive()).isTrue();
            assertThat(pos.isPositiveOrZero()).isTrue();
            assertThat(pos.isZero()).isFalse();

            assertThat(zero.isPositive()).isFalse();
            assertThat(zero.isPositiveOrZero()).isTrue();
            assertThat(zero.isZero()).isTrue();

            assertThat(neg.isPositive()).isFalse();
            assertThat(neg.isPositiveOrZero()).isFalse();
            assertThat(neg.isZero()).isFalse();

            assertThat(pos.isGreaterThan(zero)).isTrue();
            assertThat(pos.isGreaterThanOrEqual(pos)).isTrue();
            assertThat(neg.isLessThan(zero)).isTrue();
            assertThat(neg.isLessThanOrEqual(neg)).isTrue();
        }
    }

    @Nested
    @DisplayName("Pro-Rata Distribution (Hare-Niemeyer Algorithm - INV-004)")
    class DistributionAlgorithm {

        @Test
        @DisplayName("Should distribute R$ 10.00 equally across 3 items without penny loss (3.34, 3.33, 3.33)")
        void shouldDistributeOddAmountEqually() {
            Money total = Money.of("10.00");
            List<BigDecimal> weights = List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

            List<Money> shares = total.distribute(weights);

            assertThat(shares).hasSize(3);
            assertThat(shares.get(0)).isEqualTo(Money.of("3.34"));
            assertThat(shares.get(1)).isEqualTo(Money.of("3.33"));
            assertThat(shares.get(2)).isEqualTo(Money.of("3.33"));

            BigDecimal sum = shares.stream()
                    .map(Money::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(total.amount());
        }

        @Test
        @DisplayName("Should distribute R$ 100.00 with weighted proportions (30, 30, 40)")
        void shouldDistributeWeightedProportions() {
            Money total = Money.of("100.00");
            List<BigDecimal> weights = List.of(
                    new BigDecimal("30"),
                    new BigDecimal("30"),
                    new BigDecimal("40")
            );

            List<Money> shares = total.distribute(weights);

            assertThat(shares).hasSize(3);
            assertThat(shares.get(0)).isEqualTo(Money.of("30.00"));
            assertThat(shares.get(1)).isEqualTo(Money.of("30.00"));
            assertThat(shares.get(2)).isEqualTo(Money.of("40.00"));

            BigDecimal sum = shares.stream()
                    .map(Money::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(total.amount());
        }

        @Test
        @DisplayName("Should distribute R$ 0.05 across 7 items preserving total sum")
        void shouldDistributeSmallAmountAcrossManyItems() {
            Money total = Money.of("0.05");
            List<BigDecimal> weights = List.of(
                    BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE
            );

            List<Money> shares = total.distribute(weights);

            assertThat(shares).hasSize(7);
            // 5 items should get 0.01, 2 items should get 0.00
            long countCent = shares.stream().filter(m -> m.equals(Money.of("0.01"))).count();
            long countZero = shares.stream().filter(m -> m.equals(Money.of("0.00"))).count();

            assertThat(countCent).isEqualTo(5);
            assertThat(countZero).isEqualTo(2);

            BigDecimal sum = shares.stream()
                    .map(Money::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(total.amount());
        }

        @Test
        @DisplayName("Should throw exception when weights are invalid")
        void shouldThrowOnInvalidWeights() {
            Money total = Money.of("100.00");

            assertThatThrownBy(() -> total.distribute(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> total.distribute(null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> total.distribute(List.of(new BigDecimal("-1.00"), BigDecimal.ONE)))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> total.distribute(List.of(BigDecimal.ZERO, BigDecimal.ZERO)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
