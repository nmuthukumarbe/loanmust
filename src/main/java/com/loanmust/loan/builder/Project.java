package com.loanmust.loan.builder;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A builder's project. {@code approvedLenders} records which lenders have approved the
 * project (APF) - buyers in an approved project get faster, simpler legal and technical
 * checks, so the advisor and the loan file show it.
 */
@Entity
@Table(name = "builder_projects")
public class Project extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long builderId;
    private String builderName;

    @Column(nullable = false)
    private String name;

    private String location;
    private String city;
    private String reraNumber;

    /** Apartment, villa, plot, commercial ... */
    private String projectType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private ProjectStatus status = ProjectStatus.UNDER_CONSTRUCTION;

    private Integer totalUnits;
    private BigDecimal priceFrom;
    private BigDecimal priceTo;
    private LocalDate possessionDate;

    /** Lenders that have approved this project (APF), comma separated. */
    @Column(length = 1000)
    private String approvedLenders;

    @Column(length = 1000)
    private String notes;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getBuilderId() { return builderId; }
    public void setBuilderId(Long builderId) { this.builderId = builderId; }

    public String getBuilderName() { return builderName; }
    public void setBuilderName(String builderName) { this.builderName = builderName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getReraNumber() { return reraNumber; }
    public void setReraNumber(String reraNumber) { this.reraNumber = reraNumber; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public Integer getTotalUnits() { return totalUnits; }
    public void setTotalUnits(Integer totalUnits) { this.totalUnits = totalUnits; }

    public BigDecimal getPriceFrom() { return priceFrom; }
    public void setPriceFrom(BigDecimal priceFrom) { this.priceFrom = priceFrom; }

    public BigDecimal getPriceTo() { return priceTo; }
    public void setPriceTo(BigDecimal priceTo) { this.priceTo = priceTo; }

    public LocalDate getPossessionDate() { return possessionDate; }
    public void setPossessionDate(LocalDate possessionDate) { this.possessionDate = possessionDate; }

    public String getApprovedLenders() { return approvedLenders; }
    public void setApprovedLenders(String approvedLenders) { this.approvedLenders = approvedLenders; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @jakarta.persistence.Transient
    public String getStatusLabel() { return status == null ? "" : status.getLabel(); }

    @jakarta.persistence.Transient
    public String getStatusTone() { return status == null ? "slate" : status.getTone(); }

    @jakarta.persistence.Transient
    public java.util.List<String> getApprovedLenderList() {
        if (approvedLenders == null || approvedLenders.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(approvedLenders.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
