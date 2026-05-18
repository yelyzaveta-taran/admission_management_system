package com.admissionmanagement.domain.program;

public class EducationalProgram {
    private final Integer programId;
    private final String name;
    private final String description;
    private final Integer durationMonths;
    private final ComplexityLevel complexityLevel;

    public EducationalProgram(
            Integer programId,
            String name,
            String description,
            Integer durationMonths,
            ComplexityLevel complexityLevel
    ) {
        this.programId = programId;
        this.name = name;
        this.description = description;
        this.durationMonths = durationMonths;
        this.complexityLevel = complexityLevel;
    }

    public Integer getProgramId() {
        return programId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public ComplexityLevel getComplexityLevel() {
        return complexityLevel;
    }
}
