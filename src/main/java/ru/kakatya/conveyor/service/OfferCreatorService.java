package ru.kakatya.conveyor.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.kakatya.conveyor.dto.LoanApplicationRequestDTO;
import ru.kakatya.conveyor.dto.LoanOfferDTO;
import ru.kakatya.conveyor.utils.CalculateCreditUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class OfferCreatorService {
    private static final Logger LOGGER = LogManager.getLogger(OfferCreatorService.class);

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


                offerDTOList.add(createLoanOfferDto(Long.getLong("1"), calculateDefaultRate(),
                        loanApplicationRequestDTO.getTerm(), false,
                        false, loanApplicationRequestDTO.getAmount().setScale(2,RoundingMode.HALF_UP),
                        CalculateCreditUtil.calculateMonthlyPayment(loanApplicationRequestDTO.getAmount(), calculateDefaultRate(),
                                loanApplicationRequestDTO.getTerm()).setScale(2,RoundingMode.HALF_UP), calculateTotalAmount(loanApplicationRequestDTO.getAmount().setScale(2,RoundingMode.HALF_UP),
                                calculateDefaultRate(), loanApplicationRequestDTO.getTerm())));

                offerDTOList.add(createLoanOfferDto(Long.getLong("1"), calculateInsuranceClientRate(),
                        loanApplicationRequestDTO.getTerm(), false,
                        true, loanApplicationRequestDTO.getAmount(),
                        CalculateCreditUtil.calculateMonthlyPayment(loanApplicationRequestDTO.getAmount(), calculateInsuranceClientRate().setScale(2),
                                loanApplicationRequestDTO.getTerm()).setScale(2,RoundingMode.HALF_UP),
                        calculateTotalAmount(loanApplicationRequestDTO.getAmount(),
                                calculateInsuranceClientRate(), loanApplicationRequestDTO.getTerm()).setScale(2,RoundingMode.HALF_UP)));

                offerDTOList.add(createLoanOfferDto(Long.getLong("1"), calculateSalaryClientRate(),
                        loanApplicationRequestDTO.getTerm(), true,
                        false, loanApplicationRequestDTO.getAmount(),
                        CalculateCreditUtil.calculateMonthlyPayment(loanApplicationRequestDTO.getAmount(), calculateSalaryClientRate().setScale(2),
                                loanApplicationRequestDTO.getTerm()).setScale(2,RoundingMode.HALF_UP),
                        calculateTotalAmount(loanApplicationRequestDTO.getAmount(),
                                calculateSalaryClientRate(), loanApplicationRequestDTO.getTerm()).setScale(2,RoundingMode.HALF_UP)));

                offerDTOList.add(createLoanOfferDto(Long.getLong("1"), calculateInsuranceAndSalaryClientRate(),
                        loanApplicationRequestDTO.getTerm(), true,
                        true, loanApplicationRequestDTO.getAmount(),
                        CalculateCreditUtil.calculateMonthlyPayment(loanApplicationRequestDTO.getAmount(), calculateInsuranceAndSalaryClientRate().setScale(2),
                                loanApplicationRequestDTO.getTerm()).setScale(2,RoundingMode.HALF_UP),
                        calculateTotalAmount(loanApplicationRequestDTO.getAmount(),
                                calculateInsuranceAndSalaryClientRate().setScale(2), loanApplicationRequestDTO.getTerm()).setScale(2,RoundingMode.HALF_UP)));
                return offerDTOList;

            } else
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            LOGGER.error(ex.getMessage());
        }
        return Collections.emptyList();
    }

    private BigDecimal calculateTotalAmount(BigDecimal amount, BigDecimal rate, Integer term) {
        LOGGER.info("Calculate total amount");

        //p=rate/100/12
        BigDecimal monthlyPay = CalculateCreditUtil.calculateMonthlyPayment(amount, rate, term);
        BigDecimal p = rate.setScale(8, RoundingMode.HALF_DOWN)
                .divide(new BigDecimal("100.0"), RoundingMode.HALF_DOWN).setScale(8)
                .divide(new BigDecimal("12.0"), RoundingMode.HALF_DOWN).setScale(8);
        BigDecimal balanceOwed = amount.setScale(24, RoundingMode.HALF_UP);
        BigDecimal summ = new BigDecimal("0").setScale(24, RoundingMode.HALF_UP);
        BigDecimal percentage;
        BigDecimal debtPart;


        for (int i = 0; i < term; i++) {
            percentage = balanceOwed.multiply(p).setScale(24, RoundingMode.HALF_UP);
            debtPart = monthlyPay.subtract(percentage).setScale(24, RoundingMode.HALF_UP);
            balanceOwed = balanceOwed.subtract(debtPart).setScale(24, RoundingMode.HALF_UP);
            monthlyPay = debtPart.add(percentage).setScale(24, RoundingMode.HALF_UP);
            summ = summ.add(monthlyPay).setScale(24, RoundingMode.HALF_UP);
        }
        LOGGER.info("Total amount is: " + summ);
        return summ;
    }

    private LoanOfferDTO createLoanOfferDto(Long applicationId, BigDecimal rate,
                                            Integer term, Boolean isSalaryClient,
                                            Boolean isInsuranceEnabled, BigDecimal requestedAmount,
                                            BigDecimal monthlyPayment, BigDecimal totalAmount) {
        LOGGER.info("Create LoanOfferDto object");
        LoanOfferDTO loanOfferDTO = new LoanOfferDTO();
        loanOfferDTO.setApplicationId(applicationId);
        loanOfferDTO.setRate(rate);
        loanOfferDTO.setTerm(term);
        loanOfferDTO.setIsSalaryClient(isSalaryClient);
        loanOfferDTO.setIsInsuranceEnabled(isInsuranceEnabled);
        loanOfferDTO.setRequestedAmount(requestedAmount);
        loanOfferDTO.setMonthlyPayment(monthlyPayment);
        loanOfferDTO.setTotalAmount(totalAmount);
        return loanOfferDTO;
    }

    private BigDecimal calculateInsuranceAndSalaryClientRate() {
        BigDecimal rate = calculateDefaultRate()
                .subtract(new BigDecimal(deductibleInsuranceClientRate).setScale(2, RoundingMode.HALF_UP))
                .subtract(new BigDecimal(deductibleSalaryClientRate).setScale(2, RoundingMode.HALF_UP));
        LOGGER.info("Calculate rate for insurance and salary client: " + rate);
        return rate;
    }

    private BigDecimal calculateInsuranceClientRate() {
        BigDecimal rate = calculateDefaultRate()
                .subtract(new BigDecimal(deductibleInsuranceClientRate).setScale(2, RoundingMode.HALF_UP));
        LOGGER.info("Calculate rate for insurance client: ");

        return rate;
    }

    private BigDecimal calculateSalaryClientRate() {
        BigDecimal rate = calculateDefaultRate()
                .subtract(new BigDecimal(deductibleSalaryClientRate).setScale(2, RoundingMode.HALF_UP));
        LOGGER.info("Calculate rate for salary client: " + rate);
        return rate;
    }

    private BigDecimal calculateDefaultRate() {
        BigDecimal rate = new BigDecimal(centralBankRate).setScale(2, RoundingMode.HALF_UP)
                .add(new BigDecimal(defaultAdditionalRate).setScale(2, RoundingMode.HALF_UP));
        LOGGER.info("Calculate default rate of bank");
        return rate;
    }

}
