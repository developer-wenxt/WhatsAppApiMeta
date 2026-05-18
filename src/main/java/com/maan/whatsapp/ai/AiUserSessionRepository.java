package com.maan.whatsapp.ai;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiUserSessionRepository
        extends JpaRepository<AiUserSession, Long> {

    Optional<AiUserSession> findByMobileNo(String mobileNo);
}