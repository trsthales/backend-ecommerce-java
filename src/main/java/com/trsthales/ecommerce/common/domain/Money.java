package com.trsthales.ecommerce.common.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public record Money(BigDecimal amount, Currency currency) implements Comparable<Money>, Serializable {

    public static final Currency BRL = Currency.getInstance("BRL");
    public static final Money ZERO = Money.of(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "Amount é obrigatório");
        Objects.requireNonNull(currency, "Currency é obrigatória");
        if (amount.scale() != 2) {
            amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, BRL);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount), BRL);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public Money plus(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money minus(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Factor é obrigatório");
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isPositiveOrZero() {
        return this.amount.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        return this.compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isLessThan(Money other) {
        return this.compareTo(other) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        return this.compareTo(other) <= 0;
    }

    /**
     * Distribui o montante proporcionalmente de acordo com os pesos (Algoritmo Hare-Niemeyer),
     * garantindo que a soma dos itens seja EXATAMENTE igual ao total original sem perda de centavos.
     */
    public List<Money> distribute(List<BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Lista de pesos não pode ser nula ou vazia");
        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        for (BigDecimal weight : weights) {
            if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Pesos não podem ser nulos ou negativos");
            }
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Peso total deve ser estritamente positivo");
        }

        List<BigDecimal> rawAmounts = new ArrayList<>(weights.size());
        List<BigDecimal> remainders = new ArrayList<>(weights.size());
        BigDecimal distributedSum = BigDecimal.ZERO;

        for (BigDecimal weight : weights) {
            BigDecimal exactShare = this.amount.multiply(weight).divide(totalWeight, 8, RoundingMode.HALF_EVEN);
            BigDecimal baseShare = exactShare.setScale(2, RoundingMode.FLOOR);
            rawAmounts.add(baseShare);
            remainders.add(exactShare.subtract(baseShare));
            distributedSum = distributedSum.add(baseShare);
        }

        BigDecimal remainderToDistribute = this.amount.subtract(distributedSum);
        int centsToAdd = remainderToDistribute.movePointRight(2).intValueExact();

        Integer[] order = IntStream.range(0, remainders.size()).boxed()
                .sorted((i, j) -> {
                    int cmp = remainders.get(j).compareTo(remainders.get(i));
                    return cmp != 0 ? cmp : Integer.compare(i, j);
                })
                .toArray(Integer[]::new);

        for (int i = 0; i < centsToAdd; i++) {
            int idx = order[i];
            rawAmounts.set(idx, rawAmounts.get(idx).add(new BigDecimal("0.01")));
        }

        return rawAmounts.stream().map(a -> new Money(a, this.currency)).toList();
    }

    private void assertSameCurrency(Money other) {
        Objects.requireNonNull(other, "Outro valor monetário não pode ser nulo");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Moedas incompatíveis: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }
}
