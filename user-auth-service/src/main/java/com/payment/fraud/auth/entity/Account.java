package com.payment.fraud.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
@Entity
@Table(name = "account")
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;

    private String ifscCode;

    private Double balance;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "account_enabled")
    private Boolean accountEnabled;
}
