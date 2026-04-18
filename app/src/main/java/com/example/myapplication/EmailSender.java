package com.example.myapplication;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    // ⚠️ Use an App Password from Google (not your real Gmail password)
    // Go to: myaccount.google.com → Security → 2-Step Verification → App passwords
    private static final String SENDER_EMAIL    = "sirehate123@gmail.com"; // your Gmail
    private static final String SENDER_PASSWORD = "fmhc xtwo fbza dpji"; // paste App Password here (no spaces)

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static void sendPasswordReset(String toEmail, String tempPassword, EmailCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "stayFinder"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                message.setSubject("stayFinder — Your Temporary Password");
                message.setContent(buildEmailBody(tempPassword), "text/html; charset=utf-8");

                Transport.send(message);

                new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);

            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(
                    () -> callback.onFailure(e.getMessage()));
            }
        });
    }

    private static String buildEmailBody(String tempPassword) {
        return "<div style='font-family:sans-serif;max-width:480px;margin:auto;padding:32px;'>"
            + "<h2 style='color:#FF385C;'>stayFinder</h2>"
            + "<h3>Password Reset Request</h3>"
            + "<p>We received a request to reset your password. Here is your temporary password:</p>"
            + "<div style='background:#F7F7F7;border-radius:8px;padding:20px;text-align:center;"
            + "font-size:28px;font-weight:bold;letter-spacing:4px;color:#222;margin:24px 0;'>"
            + tempPassword
            + "</div>"
            + "<p>Please log in with this temporary password and change it immediately in your profile settings.</p>"
            + "<p style='color:#717171;font-size:12px;'>If you did not request this, please ignore this email. "
            + "Your account is safe.</p>"
            + "<hr style='border:none;border-top:1px solid #F0F0F0;margin:24px 0;'/>"
            + "<p style='color:#AAAAAA;font-size:11px;'>stayFinder — Keys, Not Complications</p>"
            + "</div>";
    }

    // Generate a random 8-character temporary password
    public static String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
