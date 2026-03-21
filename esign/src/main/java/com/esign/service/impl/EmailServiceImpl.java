package com.esign.service.impl;

import com.esign.event.EmailEvent;
import com.esign.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final ApplicationEventPublisher eventPublisher;

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

    // listener tetap di sini, jalan setelah transaksi commit
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailEvent(EmailEvent event) {
        sendEmail(event.to(), event.subject(), event.text());
    }
}
