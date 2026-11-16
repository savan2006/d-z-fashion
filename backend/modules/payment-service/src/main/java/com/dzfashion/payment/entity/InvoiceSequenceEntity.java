package com.dzfashion.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice_sequences")
public class InvoiceSequenceEntity {

    @Id
    @Column(name = "fiscal_year", nullable = false, length = 10)
    private String fiscalYear;

    @Column(name = "receipt_sequence", nullable = false)
    private long receiptSequence = 0;

    @Column(name = "tax_invoice_sequence", nullable = false)
    private long taxInvoiceSequence = 0;

    public InvoiceSequenceEntity() {
    }

    public InvoiceSequenceEntity(String fiscalYear) {
        this.fiscalYear = fiscalYear;
        this.receiptSequence = 0;
        this.taxInvoiceSequence = 0;
    }

    public String getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public long getReceiptSequence() {
        return receiptSequence;
    }

    public void setReceiptSequence(long receiptSequence) {
        this.receiptSequence = receiptSequence;
    }

    public long getTaxInvoiceSequence() {
        return taxInvoiceSequence;
    }

    public void setTaxInvoiceSequence(long taxInvoiceSequence) {
        this.taxInvoiceSequence = taxInvoiceSequence;
    }

    public long incrementReceiptSequence() {
        this.receiptSequence++;
        return this.receiptSequence;
    }

    public long incrementTaxInvoiceSequence() {
        this.taxInvoiceSequence++;
        return this.taxInvoiceSequence;
    }
}
