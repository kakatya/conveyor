package ru.kakatya.conveyor.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.kakatya.conveyor.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class ConveyorService {
    private static final Logger LOGGER = LogManager.getLogger(ConveyorService.class);

    @Value("${offers.default-additional-rate}")
    private String defaultAdditionalRate;
    @Value("${offers.central-bank-rate}")
    private String centralBankRate;
    @Value("${offers.deductible-insurance-client-rate}")
    private String deductibleInsuranceClientRate;

    @Value("${offers.deductible-salary-client-rate}")
    private String deductibleSalaryClientRate;

    public List<LoanOfferDTO> evaluateClient(LoanApplicationRequestDTO loanApplicationRequestDTO) {
        List<LoanOfferDTO> offerDTOList = new LinkedList<>();
        LOGGER.info("Creation of loan offers");
        try {
            if (loanApplicationRequestDTO.getAmount().signum() == 1) {


                offerDTOList.add(createLoanOfferDto(Long.parseLong("1"), calculateDefaultRate(),
                        loanApplicationRequestDTO.getTerm(), false,
                        false, loanApplicationRequestDTO.getAmount()));

                offerDTOList.add(createLoanOfferDto(Long.parseLong("1"), calculateInsuranceClientRate(),
                        loanApplicationRequestDTO.getTerm(), false,
                        true, loanApplicationRequestDTO.getAmount()));

                offerDTOList.add(createLoanOfferDto(Long.parseLong("1"), calculateSalaryClientRate(),
                        loanApplicationRequestDTO.getTerm(), true,
                        false, loanApplicationRequestDTO.getAmount()));

                offerDTOList.add(createLoanOfferDto(Long.parseLong("1"), calculateInsuranceAndSalaryClientRate(),
                        loanApplicationRequestDTO.getTerm(), true,
                        true, loanApplicationRequestDTO.getAmount()));
                return offerDTOList;

            } else
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            LOGGER.error(ex.getMessage());
        }
        return Collections.emptyList();
    }

    public CreditDTO creditCalculation(ScoringDataDTO scoringDataDTO) {
        LOGGER.info("Calculate a loan");
        CreditDTO creditDTO = new CreditDTO();
        creditDTO.setRate(calculateRate(scoringDataDTO.getIsSalaryClient(), scoringDataDTO.getIsInsuranceEnabled()));
        creditDTO.setAmount(scoringDataDTO.getAmount());
        creditDTO.setMonthlyPayment(calculateMonthlyPayment(scoringDataDTO.getAmount(), creditDTO.getRate(), scoringDataDTO.getTerm())
                .setScale(2, RoundingMode.HALF_UP));
        creditDTO.setTerm(scoringDataDTO.getTerm());
        creditDTO.setIsSalaryClient(scoringDataDTO.getIsSalaryClient());
        creditDTO.setIsInsuranceEnabled(scoringDataDTO.getIsInsuranceEnabled());
        creditDTO.setPsk(calculatePsk(creditDTO.getMonthlyPayment(), creditDTO.getRate(), creditDTO.getTerm())
                .setScale(2, RoundingMode.HALF_UP));
        creditDTO.setPaymentSchedule(calculatePaymentSchedule(creditDTO.getAmount(), creditDTO.getRate(), creditDTO.getTerm()));
        return creditDTO;
    }

    private List<PaymentScheduleElementDto> calculatePaymentSchedule(BigDecimal amount, BigDecimal rate, Integer term) {
        List<PaymentScheduleElementDto> paymentScheduleElementDtos = new LinkedList<>();
        BigDecimal monthlyPay = calculateMonthlyPayment(amount, rate, term);
        LocalDate localDate = LocalDate.now();
        LOGGER.info("Calculate Payment schedule");
        //p=rate/100/12
        BigDecimal p = rate.setScale(8, RoundingMode.HALF_DOWN)
                .divide(new BigDecimal("100"), RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_UP);
        BigDecimal remainingDebt = amount.setScale(24, RoundingMode.HALF_UP);
        BigDecimal totalPayment = BigDecimal.ZERO.setScale(24, RoundingMode.HALF_UP);
        BigDecimal interestPayment;
        BigDecimal debtPayment;

        for (int i = 0; i < term; i++) {
            PaymentScheduleElementDto paymentScheduleElementDto = new PaymentScheduleElementDto();

            interestPayment = remainingDebt.multiply(p).setScale(24, RoundingMode.HALF_UP);
            debtPayment = monthlyPay.subtract(interestPayment).setScale(24, RoundingMode.HALF_UP);
            remainingDebt = remainingDebt.subtract(debtPayment).setScale(24, RoundingMode.HALF_UP);
            monthlyPay = debtPayment.add(interestPayment).setScale(24, RoundingMode.HALF_UP);
            totalPayment = totalPayment.add(monthlyPay).setScale(24, RoundingMode.HALF_UP);
            localDate = localDate.plusMonths(1);

            paymentScheduleElementDto.setNumber(i + 1);
            paymentScheduleElementDto.setRemainingDebt(remainingDebt.setScale(2, RoundingMode.HALF_UP));
            paymentScheduleElementDto.setTotalPayment(totalPayment.setScale(2, RoundingMode.HALF_UP));
            paymentScheduleElementDto.setInterestPayment(interestPayment.setScale(2, RoundingMode.HALF_UP));
            paymentScheduleElementDto.setDate(localDate);
            paymentScheduleElementDto.setDebtPayment(debtPayment.setScale(2, RoundingMode.HALF_UP));
            paymentScheduleElementDtos.add(paymentScheduleElementDto);
        }
        return paymentScheduleElementDtos;
    }

    private BigDecimal calculateRate(boolean isSalaryEnabled, boolean isInsuranceEnabled) {
        LOGGER.info("Calculate rate");
        if (isSalaryEnabled && isInsuranceEnabled) {
            return calculateInsuranceAndSalaryClientRate();
        } else if (isSalaryEnabled) {
            return calculateSalaryClientRate();
        } else if (isInsuranceEnabled) {
            return calculateInsuranceClientRate();
        }
        return calculateDefaultRate();
    }

    private BigDecimal calculatePsk(BigDecimal amount, BigDecimal rate, int term) {
        BigDecimal psk = calculateTotalAmount(amount, rate, term).setScale(24, RoundingMode.HALF_UP)
                .divide(amount.setScale(24, RoundingMode.HALF_UP), RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .divide(new BigDecimal(Integer.toString(term)).setScale(0, RoundingMode.HALF_UP), RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100").setScale(0, RoundingMode.HALF_UP));
        LOGGER.info("Calculate PSK: {}", psk);
        return psk;
    }

    private BigDecimal calculateTotalAmount(BigDecimal amount, BigDecimal rate, Integer term) {
        LOGGER.info("Calculate total amount");

        BigDecimal monthlyPay = calculateMonthlyPayment(amount, rate, term);
        //p=rate/100/12
        BigDecimal p = rate.setScale(8, RoundingMode.HALF_DOWN)
                .divide(new BigDecimal("100"), RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_UP);
        BigDecimal balanceOwed = amount.setScale(24, RoundingMode.HALF_UP);
        BigDecimal summ = BigDecimal.ZERO.setScale(24, RoundingMode.HALF_UP);
        BigDecimal percentage;
        BigDecimal debtPart;


        for (int i = 0; i < term; i++) {
            percentage = balanceOwed.multiply(p).setScale(24, RoundingMode.HALF_UP);
            debtPart = monthlyPay.subtract(percentage).setScale(24, RoundingMode.HALF_UP);
            balanceOwed = balanceOwed.subtract(debtPart).setScale(24, RoundingMode.HALF_UP);
            monthlyPay = debtPart.add(percentage).setScale(24, RoundingMode.HALF_UP);
            summ = summ.add(monthlyPay).setScale(24, RoundingMode.HALF_UP);
        }
        LOGGER.info("Total amount is: {}", summ);
        return summ;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal rate, Integer term) {
        LOGGER.info("Calculate monthly payment");
        //p=rate/100/12
        BigDecimal p = rate.setScale(8, RoundingMode.HALF_DOWN)
                .divide(new BigDecimal("100.0"), RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12.0"), RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_UP);

        // (1+p)^term
        BigDecimal denominatorMinuend = p.add(BigDecimal.ONE).pow(term)
                .setScale(24, RoundingMode.HALF_UP);

        // denominator=denominatorMinuend-1
        BigDecimal denominator = denominatorMinuend.subtract(BigDecimal.ONE)
                .setScale(24, RoundingMode.HALF_UP);

        // fraction=p/denominator
        BigDecimal fraction = p.divide(denominator, RoundingMode.HALF_UP).setScale(24, RoundingMode.HALF_UP);

        //secondMultiplier=p+fraction
        BigDecimal secondMultiplier = p.add(fraction)
                .setScale(24, RoundingMode.HALF_UP);
        BigDecimal result = amount.setScale(24, RoundingMode.HALF_UP)
                .multiply(secondMultiplier).setScale(24, RoundingMode.HALF_UP);
        LOGGER.info("MonthlyPayment is: {}", result);
        return result;
    }

    private LoanOfferDTO createLoanOfferDto(Long applicationId, BigDecimal rate,
                                            Integer term, Boolean isSalaryClient,
                                            Boolean isInsuranceEnabled, BigDecimal requestedAmount) {
        LOGGER.info("Create LoanOfferDto object");
        LoanOfferDTO loanOfferDTO = new LoanOfferDTO();
        loanOfferDTO.setApplicationId(applicationId);
        loanOfferDTO.setRate(rate);
        loanOfferDTO.setTerm(term);
        loanOfferDTO.setIsSalaryClient(isSalaryClient);
        loanOfferDTO.setIsInsuranceEnabled(isInsuranceEnabled);
        loanOfferDTO.setRequestedAmount(requestedAmount);
        loanOfferDTO.setMonthlyPayment(calculateMonthlyPayment(requestedAmount, rate, term).setScale(2, RoundingMode.HALF_UP));
        loanOfferDTO.setTotalAmount(calculateTotalAmount(requestedAmount, rate, term).setScale(2, RoundingMode.HALF_UP));
        return loanOfferDTO;
    }

    private BigDecimal calculateInsuranceAndSalaryClientRate() {
        BigDecimal rate = calculateDefaultRate()
                .subtract(new BigDecimal(deductibleInsuranceClientRate).setScale(2, RoundingMode.HALF_UP))
                .subtract(new BigDecimal(deductibleSalaryClientRate).setScale(2, RoundingMode.HALF_UP));
        LOGGER.info("Calculate rate for insurance and salary client: {}%", rate);
        return rate;
    }

    private BigDecimal calculateInsuranceClientRate() {
        BigDecimal rate = calculateDefaultRate()
                .subtract(new BigDecimal(deductibleInsuranceClientRate).setScale(2, RoundingMode.HALF_UP));
        LOGGER.info("Calculate rate for insurance client: {}%", rate);

        return rate;
    }

    private BigDecimal calculateSalaryClientRate() {
        BigDecimal rate = calculateDefaultRate()
                .subtract(new BigDecimal(deductibleSalaryClientRate).setScale(2, RoundingMode.HALF_UP));
        LOGGER.info("Calculate rate for salary client: {}%", rate);
        return rate;
    }

    private BigDecimal calculateDefaultRate() {
        BigDecimal rate = new BigDecimal(centralBankRate).setScale(2, RoundingMode.HALF_UP)
                .add(new BigDecimal(defaultAdditionalRate).setScale(2, RoundingMode.HALF_UP));
        LOGGER.info("Calculate default rate of bank: {}%", rate);
        return rate;
    }

}
