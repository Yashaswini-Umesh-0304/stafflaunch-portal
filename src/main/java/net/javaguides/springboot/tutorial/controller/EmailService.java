package net.javaguides.springboot.tutorial.controller;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture; // Added for background processing

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String adminEmail = "yashaswiniumesh157@gmail.com";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendHtmlEmail(String toEmail, String subject, String title, String bodyContent, String buttonText, String buttonUrl) {
        // Runs the email task in a background thread so the UI never freezes!
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(adminEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);

                String buttonHtml = "";
                if (buttonText != null && buttonUrl != null) {
                    buttonHtml = "<div style='margin-top: 30px;'><a href='" + buttonUrl + "' style='background-color: #0d6efd; color: #ffffff; padding: 12px 25px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; display: inline-block;'>" + buttonText + "</a></div>";
                }

                String htmlTemplate = ""
                    + "<div style='background-color: #cfd2d6; padding: 40px 20px; font-family: Arial, sans-serif;'>"
                    + "  <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; padding: 40px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);'>"
                    + "    <h4 style='color: #495057; margin-top: 0; font-size: 13px; text-transform: uppercase; letter-spacing: 1px; font-weight: bold;'>STAFFLAUNCH PORTAL</h4>"
                    + "    <h2 style='color: #212529; font-size: 26px; margin-bottom: 20px; font-weight: bold;'>" + title + "</h2>"
                    + "    <div style='color: #495057; font-size: 16px; line-height: 1.6;'>"
                    + bodyContent
                    + "    </div>"
                    + buttonHtml
                    + "  </div>"
                    + "</div>";

                helper.setText(htmlTemplate, true);
                mailSender.send(message);
                
            } catch (Exception e) {
                // Catching Exception (not just MessagingException) prevents the 500 Server Error
                System.out.println("Background email failed quietly: " + e.getMessage());
            }
        });
    }
}