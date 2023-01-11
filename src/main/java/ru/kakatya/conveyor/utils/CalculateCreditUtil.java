package ru.kakatya.conveyor.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculateCreditUtil {
    private static final Logger LOGGER = LogManager.getLogger(CalculateCreditUtil.class);

    public static BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal rate, Integer term) {
        LOGGER.info("Calculate monthly payment");
        //p=rate/100/12
        BigDecimal p = rate.setScale(8, RoundingMode.HALF_DOWN)
                .divide(new BigDecimal("100.0"), RoundingMode.HALF_DOWN).setScale(8)
                .divide(new BigDecimal("12.0"), RoundingMode.HALF_DOWN).setScale(8);

        // (1+p)^term
        BigDecimal denominatorMinuend = p.add(new BigDecimal("1.0")).pow(term)
                .setScale(24, RoundingMode.HALF_UP);

        // denominator=denominatorMinuend-1
        BigDecimal denominator = denominatorMinuend.subtract(new BigDecimal("1"))
                .setScale(24, RoundingMode.HALF_UP);

        // fraction=p/denominator
        BigDecimal fraction = p.divide(denominator, RoundingMode.HALF_UP).setScale(24);

        //secondMultiplier=p+fraction
        BigDecimal secondMultiplier = p.add(fraction)
                .setScale(24, RoundingMode.HALF_UP);
        BigDecimal result = amount.setScale(24, RoundingMode.HALF_UP)
                .multiply(secondMultiplier).setScale(24, RoundingMode.HALF_UP);
        LOGGER.info("MonthlyPayment is: " + result);
        return result;
    }
}
