package ru.kakatya.conveyor.dto;

import lombok.Data;
import ru.kakatya.conveyor.dto.enums.EmploymentStatus;
import ru.kakatya.conveyor.dto.enums.Position;

import java.math.BigDecimal;

@Data
public class EmploymentDTO {
   private EmploymentStatus employmentStatus;
   private String employerINN;
   private BigDecimal salary;
   private Position position;
   private Integer workExperienceTotal;
   private Integer workExperienceCurrent;
}
