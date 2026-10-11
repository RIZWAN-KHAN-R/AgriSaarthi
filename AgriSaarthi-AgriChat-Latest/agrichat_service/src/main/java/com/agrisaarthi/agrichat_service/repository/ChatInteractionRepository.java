package com.agrisaarthi.agrichat_service.repository;

import com.agrisaarthi.agrichat_service.model.ChatInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatInteractionRepository extends JpaRepository<ChatInteraction, Long> {
    List<ChatInteraction> findTop50ByConversationIdOrderByCreatedAtDesc(String conversationId);
}
