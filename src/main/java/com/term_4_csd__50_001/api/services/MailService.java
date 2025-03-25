package com.term_4_csd__50_001.api.services;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.term_4_csd__50_001.api.Dotenv;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;
import jakarta.mail.*;
import jakarta.mail.internet.*;

@Service
public class MailService {

    private Session session;
    private String mailUsername;

    @Autowired
    public MailService(Dotenv dotenv) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", dotenv.get(Dotenv.MAIL_SMTP_AUTH));
        props.put("mail.smtp.starttls.enable", dotenv.get(Dotenv.MAIL_SMTP_STARTTLS));
        props.put("mail.smtp.host", dotenv.get(Dotenv.MAIL_HOST));
        props.put("mail.smtp.port", dotenv.get(Dotenv.MAIL_PORT));
        this.mailUsername = dotenv.get(Dotenv.MAIL_USERNAME);
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailUsername, dotenv.get(Dotenv.MAIL_PASSWORD));
            }
        });
        this.session = session;
    }

    public void sendMail(MailBuilder mailBuilder) {
        try {
            Message message = mailBuilder.build(session, mailUsername);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new InternalServerErrorException("Something went wrong sending email", e);
        }
    }

    public MailBuilder mailBuilder() {
        return new MailBuilder();
    }

    public static class MailBuilder {

        private String text;
        private String recipients;
        private String subject;


        public MailBuilder text(String text) {
            this.text = text;
            return this;
        }

        public MailBuilder recipients(String recipients) {
            this.recipients = recipients;
            return this;
        }

        public MailBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        private Message build(Session session, String from) throws MessagingException {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setSubject(subject);
            message.setText(text);
            return message;
        }

    }

}
