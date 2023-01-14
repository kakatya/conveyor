package ru.kakatya.conveyor.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kakatya.conveyor.dto.CreditDTO;
import ru.kakatya.conveyor.dto.LoanApplicationRequestDTO;
import ru.kakatya.conveyor.dto.LoanOfferDTO;
import ru.kakatya.conveyor.dto.ScoringDataDTO;
import ru.kakatya.conveyor.service.ConveyorService;

import java.util.List;

@Api(tags = "Контроллер кредитного конвейера")
@RestController
@RequestMapping("/conveyor")
public class ConveyorController {
    private final ConveyorService conveyorService;

    @Autowired
    public ConveyorController(ConveyorService offerCreatorService) {
        this.conveyorService = offerCreatorService;
    }

    @ApiOperation("Подборка кредитных предложений")
    @PostMapping("/offers")
    public ResponseEntity<List<LoanOfferDTO>> issueOffer(@RequestBody LoanApplicationRequestDTO dto) {
        List<LoanOfferDTO> loanOfferDTOS = conveyorService.evaluateClient(dto);
        return ResponseEntity.ok().body(loanOfferDTOS);
    }

    @ApiOperation("Расчет кредита")
    @PostMapping("/calculation")
    public ResponseEntity<CreditDTO> calculateCredit(@RequestBody ScoringDataDTO dto) {
        return ResponseEntity.ok().body(conveyorService.creditCalculation(dto));
    }
}
