package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.Conversation;
import com.yayfolk.backend.repository.ConversationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class HumanServiceTimeoutScheduler {

    private static final long TIMEOUT_MINUTES = 5;

    private final ConversationRepository conversationRepository;
    private final MessageService messageService;

    public HumanServiceTimeoutScheduler(ConversationRepository conversationRepository,
                                        MessageService messageService) {
        this.conversationRepository = conversationRepository;
        this.messageService = messageService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkTimeout() {
        List<Conversation> serviceConversations = conversationRepository.findAllByType("service");
        Date now = new Date();

        for (Conversation conversation : serviceConversations) {
            if (!"human".equals(conversation.getServiceMode())) {
                continue;
            }

            Date lastHumanReplyTime = conversation.getLastHumanReplyTime();
            if (lastHumanReplyTime == null) {
                long createdElapsed = now.getTime() - conversation.getCreateTime().getTime();
                if (createdElapsed > TimeUnit.MINUTES.toMillis(TIMEOUT_MINUTES)) {
                    switchBackToAI(conversation);
                }
                continue;
            }

            long elapsed = now.getTime() - lastHumanReplyTime.getTime();
            if (elapsed > TimeUnit.MINUTES.toMillis(TIMEOUT_MINUTES)) {
                switchBackToAI(conversation);
            }
        }
    }

    private void switchBackToAI(Conversation conversation) {
        conversation.setServiceMode("ai");
        conversation.setLastHumanReplyTime(null);
        conversationRepository.save(conversation);

        Long userId = conversation.getUser1Id();
        Long adminId = conversation.getUser2Id();

        String systemMsg = "人工客服已离线，已为您切换回智能客服。如需再次转人工，请输入\"转人工\"。";
        messageService.saveServiceAIMessage(conversation.getId(), adminId, userId, systemMsg);
    }
}
