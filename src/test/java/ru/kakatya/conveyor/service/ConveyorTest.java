package ru.kakatya.conveyor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.kakatya.conveyor.dto.CreditDTO;
import ru.kakatya.conveyor.dto.LoanApplicationRequestDTO;
import ru.kakatya.conveyor.dto.LoanOfferDTO;
import ru.kakatya.conveyor.dto.ScoringDataDTO;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class ConveyorTest {

    @Autowired
    ConveyorService offerCreatorService;


    @Test
    void contextLoads() {
        LoanApplicationRequestDTO dto = createLoanApplicationRequestDTO();
        List<LoanOfferDTO> loanOfferDTOS = offerCreatorService.evaluateClient(dto);
        CreditDTO creditDTO = offerCreatorService.creditCalculation(createScoringDateDto());
        List<BigDecimal> monthlyPayments = new LinkedList<>(Arrays.asList(
                new BigDecimal("9744.91").setScale(2, RoundingMode.CEILING), new BigDecimal("9602.58").setScale(2, RoundingMode.CEILING),
                new BigDecimal("9555.40").setScale(2, RoundingMode.CEILING), new BigDecimal("9414.69").setScale(2, RoundingMode.CEILING)
        ));
        boolean listSorted = true;
        for (int i = 1; i < loanOfferDTOS.size(); i++) {
            if (loanOfferDTOS.get(i - 1).getRate().compareTo(loanOfferDTOS.get(i).getRate()) < 0) {
                listSorted = false;
                break;
            }
        }
        for (int i = 0; i < loanOfferDTOS.size(); i++) {
            Assertions.assertEquals(monthlyPayments.get(i), loanOfferDTOS.get(i).getMonthlyPayment(), "Monthly pay is not correct");
        }
        Assertions.assertTrue(listSorted, "List did not sort");
        Assertions.assertEquals(creditDTO.getMonthlyPayment(), monthlyPayments.get(3), "Monthly payment is incorrect");
        Assertions.assertEquals(creditDTO.getPaymentSchedule().get(4).getDebtPayment(), new BigDecimal("7715.76"), "Debt payments is incorrect");
    }

    private LoanApplicationRequestDTO createLoanApplicationRequestDTO() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        LoanApplicationRequestDTO loanApplicationRequestDTO;
        try {
            File file = new File("src/test/resources/loanApplicationRequestDTO.json");
            loanApplicationRequestDTO = objectMapper.readValue(file, LoanApplicationRequestDTO.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return loanApplicationRequestDTO;
    }

    private ScoringDataDTO createScoringDateDto() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ScoringDataDTO scoringDataDTO;
        try {
            File file = new File("src/test/resources/scoringDataDTO.json");
            scoringDataDTO = objectMapper.readValue(file, ScoringDataDTO.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return scoringDataDTO;
    }

}
