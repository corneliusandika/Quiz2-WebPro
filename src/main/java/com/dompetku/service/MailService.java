package com.dompetku.service;

import com.dompetku.util.AppConfig;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/** SMTP mail sender. Configuration comes from environment variables only. */
public final class MailService {

    private MailService() {
    }

    public static void send(String to, String subject, String body) throws MessagingException {
        String host = AppConfig.value("DOMPETKU_SMTP_HOST", "");
        if (host.isEmpty()) {
            throw new MessagingException("SMTP is not configured");
        }
        String port = AppConfig.value("DOMPETKU_SMTP_PORT", "587");
        String user = AppConfig.value("DOMPETKU_SMTP_USER", "");
        String password = AppConfig.value("DOMPETKU_SMTP_PASSWORD", "");
        String from = AppConfig.value("DOMPETKU_SMTP_FROM", user.isEmpty() ? "no-reply@dompetku.local" : user);
        boolean auth = !user.isEmpty();
        boolean startTls = !"false".equalsIgnoreCase(AppConfig.value("DOMPETKU_SMTP_STARTTLS", "true"));
        boolean ssl = "true".equalsIgnoreCase(AppConfig.value("DOMPETKU_SMTP_SSL", "false"));

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, auth ? new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        } : null);

        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(from, "Dompetku"));
        } catch (UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(from));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");
        Transport.send(message);
    }
}
