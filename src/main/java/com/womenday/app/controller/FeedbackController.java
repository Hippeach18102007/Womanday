package com.womenday.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class FeedbackController {

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.to-email}")
    private String toEmail;

    @Value("${resend.from-email}")
    private String fromEmail;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.resend.com")
            .build();

    @PostMapping("/api/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();

        String name = String.valueOf(body.getOrDefault("name", "Ẩn danh"));
        int star = body.get("star") != null ? Integer.parseInt(String.valueOf(body.get("star"))) : 0;
        String wish = String.valueOf(body.getOrDefault("wish", "")).trim();

        if (star == 0 && wish.isEmpty()) {
            response.put("success", false);
            response.put("error", "Không có nội dung để gửi");
            return ResponseEntity.badRequest().body(response);
        }

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String stars = star > 0 ? "⭐".repeat(star) + " (" + star + "/5)" : "Chưa đánh giá";

        String html = buildEmailHtml(name, stars, wish, time);

        try {
            Map<String, Object> emailPayload = new HashMap<>();
            emailPayload.put("from", "8/3 App <" + fromEmail + ">");
            emailPayload.put("to", List.of(toEmail));
            emailPayload.put("subject", "💌 [8/3] " + name + " gửi đánh giá & điều ước");
            emailPayload.put("html", html);

            webClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(emailPayload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            response.put("success", true);
            response.put("message", "Đã gửi thành công!");
        } catch (Exception e) {
            System.err.println("Resend error: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Lỗi gửi email: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    private String buildEmailHtml(String name, String stars, String wish, String time) {
        // Dùng StringBuilder thay vì .formatted() để tránh lỗi với % và ; trong CSS
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'></head>");
        sb.append("<body style='font-family:Arial,sans-serif;background:#fdf6f0;margin:0;padding:0'>");
        sb.append("<div style='max-width:560px;margin:40px auto;background:white;border-radius:20px;overflow:hidden;box-shadow:0 8px 40px rgba(200,66,90,.12)'>");

        // Header
        sb.append("<div style='background:linear-gradient(135deg,#b83050,#c8425a,#c9a84c);padding:2rem;text-align:center'>");
        sb.append("<div style='font-size:2rem;margin-bottom:.5rem'>🌹</div>");
        sb.append("<div style='color:white;font-size:1.3rem;font-weight:700'>Phản hồi từ trang 8/3</div>");
        sb.append("<div style='color:rgba(255,255,255,.8);font-size:.85rem;margin-top:.3rem'>").append(time).append("</div>");
        sb.append("</div>");

        // Body
        sb.append("<div style='padding:2rem'>");
        sb.append("<table style='width:100%;border-collapse:collapse'>");

        // Tên
        sb.append("<tr>");
        sb.append("<td style='padding:.6rem 0;border-bottom:1px solid #fce8ec;color:#888;font-size:.85rem;width:120px'>👤 Tên</td>");
        sb.append("<td style='padding:.6rem 0;border-bottom:1px solid #fce8ec;font-weight:600;color:#333'>").append(escapeHtml(name)).append("</td>");
        sb.append("</tr>");

        // Đánh giá
        sb.append("<tr>");
        sb.append("<td style='padding:.6rem 0;border-bottom:1px solid #fce8ec;color:#888;font-size:.85rem'>⭐ Đánh giá</td>");
        sb.append("<td style='padding:.6rem 0;border-bottom:1px solid #fce8ec;font-size:1rem'>").append(stars).append("</td>");
        sb.append("</tr>");

        // Điều ước (nếu có)
        if (!wish.isEmpty()) {
            sb.append("<tr>");
            sb.append("<td style='padding:.6rem 0;color:#888;font-size:.85rem;vertical-align:top'>💌 Điều ước</td>");
            sb.append("<td style='padding:.6rem 0;color:#c8425a;font-style:italic;line-height:1.6'>").append(escapeHtml(wish)).append("</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("</div>");

        // Footer
        sb.append("<div style='background:#fff8f9;padding:1rem 2rem;text-align:center;font-size:.75rem;color:#ccc;border-top:1px dashed #f5dde4'>");
        sb.append("Gửi từ trang chúc mừng 8/3 · Đào Đức");
        sb.append("</div>");

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", "<br>");
    }
}
