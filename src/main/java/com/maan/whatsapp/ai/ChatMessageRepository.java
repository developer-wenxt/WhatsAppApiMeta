package com.maan.whatsapp.ai;



import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository
        extends JpaRepository<
        ChatMessageEntity,
        Long> {
}