package com.esign.service.impl;

import com.esign.config.RabbitMQConfig;
import com.esign.event.EmailEvent;
import com.esign.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final ApplicationEventPublisher eventPublisher;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // publish event, dipanggil dari service lain
    @Override
    public void sendEmailAfterCommit(String to, String subject, String text) {
        eventPublisher.publishEvent(new EmailEvent(to, subject, text));
    }

    // listener jalan setelah transaksi commit
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailEvent(EmailEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.EMAIL_KEY,
                event
        );
    }

    // consumer — handle pengiriman email dari queue
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE, concurrency = "3-10")
    public void consumeEmailEvent(EmailEvent event) {
        sendEmail(event.to(), event.subject(), event.text());
    }
}
