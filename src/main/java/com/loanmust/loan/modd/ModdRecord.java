package com.loanmust.loan.modd;

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
import java.time.LocalDateTime;

/** The MODD registration job for one loan file - one per file. */
@Entity
@Table(name = "modd_records")
public class ModdRecord extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    private String code;   // MD-0001

    @Column(nullable = false)
    private Long applicationId;
    private String applicationCode;
    private Long customerId;
    private String customerName;
    private String customerMobile;
    private String lenderName;

    @Column(length = 500)
    private String propertyAddress;

    /** Loan amount secured by the deposit - the base for stamp duty. */
    private BigDecimal securedAmount;

    private String subRegistrarOffice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private ModdStatus status = ModdStatus.NOT_STARTED;

    private LocalDateTime appointmentAt;
    private LocalDate registeredOn;
    /** Document / registration number issued by the sub-registrar. */
    private String registrationNumber;

    private BigDecimal stampDuty;
    private BigDecimal registrationFee;
    private BigDecimal otherCharges;
    /** The agency's own service charge to the customer. */
    private BigDecimal serviceCharge;
    private boolean chargesCollected;

    /** The original documents handed to the bank, one per line. */
    @Column(length = 2000)
    private String originalDocuments;

    private LocalDate depositedOn;
    private String depositedWith;
    private String acknowledgementNumber;

    private Long assignedUserId;
    private String assignedUserName;

    @Column(length = 1000)
    private String remarks;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getApplicationCode() { return applicationCode; }
    public void setApplicationCode(String applicationCode) { this.applicationCode = applicationCode; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }

    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }

    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }

    public BigDecimal getSecuredAmount() { return securedAmount; }
    public void setSecuredAmount(BigDecimal securedAmount) { this.securedAmount = securedAmount; }

    public String getSubRegistrarOffice() { return subRegistrarOffice; }
    public void setSubRegistrarOffice(String subRegistrarOffice) { this.subRegistrarOffice = subRegistrarOffice; }

    public ModdStatus getStatus() { return status; }
    public void setStatus(ModdStatus status) { this.status = status; }

    public LocalDateTime getAppointmentAt() { return appointmentAt; }
    public void setAppointmentAt(LocalDateTime appointmentAt) { this.appointmentAt = appointmentAt; }

    public LocalDate getRegisteredOn() { return registeredOn; }
    public void setRegisteredOn(LocalDate registeredOn) { this.registeredOn = registeredOn; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public BigDecimal getStampDuty() { return stampDuty; }
    public void setStampDuty(BigDecimal stampDuty) { this.stampDuty = stampDuty; }

    public BigDecimal getRegistrationFee() { return registrationFee; }
    public void setRegistrationFee(BigDecimal registrationFee) { this.registrationFee = registrationFee; }

    public BigDecimal getOtherCharges() { return otherCharges; }
    public void setOtherCharges(BigDecimal otherCharges) { this.otherCharges = otherCharges; }

    public BigDecimal getServiceCharge() { return serviceCharge; }
    public void setServiceCharge(BigDecimal serviceCharge) { this.serviceCharge = serviceCharge; }

    public boolean isChargesCollected() { return chargesCollected; }
    public void setChargesCollected(boolean chargesCollected) { this.chargesCollected = chargesCollected; }

    public String getOriginalDocuments() { return originalDocuments; }
    public void setOriginalDocuments(String originalDocuments) { this.originalDocuments = originalDocuments; }

    public LocalDate getDepositedOn() { return depositedOn; }
    public void setDepositedOn(LocalDate depositedOn) { this.depositedOn = depositedOn; }

    public String getDepositedWith() { return depositedWith; }
    public void setDepositedWith(String depositedWith) { this.depositedWith = depositedWith; }

    public String getAcknowledgementNumber() { return acknowledgementNumber; }
    public void setAcknowledgementNumber(String acknowledgementNumber) { this.acknowledgementNumber = acknowledgementNumber; }

    public Long getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }

    public String getAssignedUserName() { return assignedUserName; }
    public void setAssignedUserName(String assignedUserName) { this.assignedUserName = assignedUserName; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @jakarta.persistence.Transient
    public String getStatusLabel() { return status == null ? "" : status.getLabel(); }

    @jakarta.persistence.Transient
    public String getStatusTone() { return status == null ? "slate" : status.getTone(); }

    @jakarta.persistence.Transient
    public boolean isOpen() { return status != null && status.isOpen(); }

    @jakarta.persistence.Transient
    public BigDecimal getTotalGovernmentCharges() {
        return nz(stampDuty).add(nz(registrationFee)).add(nz(otherCharges));
    }

    @jakarta.persistence.Transient
    public BigDecimal getTotalToCollect() {
        return getTotalGovernmentCharges().add(nz(serviceCharge));
    }

    @jakarta.persistence.Transient
    public java.util.List<String> getOriginalDocumentList() {
        if (originalDocuments == null || originalDocuments.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(originalDocuments.split("\\r?\\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @jakarta.persistence.Transient
    public int getProgressPercent() {
        int idx = ModdStatus.path().indexOf(status);
        return idx < 0 ? 0 : Math.round(100f * (idx + 1) / ModdStatus.path().size());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
