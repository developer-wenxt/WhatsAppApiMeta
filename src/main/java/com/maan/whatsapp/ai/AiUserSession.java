package com.maan.whatsapp.ai;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_user_session")
public class AiUserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mobileNo;

    private String customerName;

    private String vehicleNo;

    private String claimType;

    private String currentStep;

    private String confirmation;

}
