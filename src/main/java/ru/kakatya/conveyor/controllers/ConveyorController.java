package ru.kakatya.conveyor.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kakatya.conveyor.dto.CreditDTO;
import ru.kakatya.conveyor.dto.LoanApplicationRequestDTO;
import ru.kakatya.conveyor.dto.LoanOfferDTO;
import ru.kakatya.conveyor.dto.ScoringDataDTO;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/conveyor")
public class ConveyorController {
    @PostMapping("/offers")
    public List<LoanOfferDTO> offer(@RequestBody LoanApplicationRequestDTO dto) {
        return Collections.emptyList();
    }

    @PostMapping("/calculation")
    public CreditDTO calculation(@RequestBody ScoringDataDTO dto) {
        return new CreditDTO();
    }
}
