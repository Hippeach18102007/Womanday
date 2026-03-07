package com.womenday.app.controller;

import com.womenday.app.model.Greeting;
import com.womenday.app.service.GreetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Controller
public class GreetingController {

    @Autowired
    private GreetingService greetingService;

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/admin")
    public String admin() { return "admin"; }

    @GetMapping("/api/greeting")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGreeting(@RequestParam String name) {
        Map<String, Object> response = new HashMap<>();
        if (name == null || name.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Tên không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        Greeting greeting = greetingService.getGreeting(name);
        response.put("success", true);
        response.put("name", greeting.getRecipientName());
        response.put("message", greeting.getMessage());
        response.put("hasPhoto", greeting.isHasPhoto());
        response.put("photoPath", greeting.getPhotoPath()); // data:image/...;base64,...
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/admin/message")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveMessage(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String name = body.get("name");
        String message = body.get("message");
        if (name == null || message == null) {
            response.put("success", false);
            response.put("error", "Thiếu tên hoặc lời chúc");
            return ResponseEntity.badRequest().body(response);
        }
        greetingService.setCustomMessage(name, message);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload ảnh → convert sang Base64 Data URL → lưu thẳng vào DB.
     * Không dùng file system → hoạt động tốt trên Koyeb / mọi cloud platform.
     */
    @PostMapping("/api/admin/photo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @RequestParam("name") String name,
            @RequestParam("file") MultipartFile file) throws IOException {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("error", "File rỗng");
            return ResponseEntity.badRequest().body(response);
        }

        // Giới hạn 3MB
        if (file.getSize() > 3 * 1024 * 1024) {
            response.put("success", false);
            response.put("error", "Ảnh quá lớn, vui lòng dùng ảnh dưới 3MB");
            return ResponseEntity.badRequest().body(response);
        }

        // Xác định MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            contentType = "image/jpeg";
        }

        // Convert sang Base64 Data URL
        byte[] bytes = file.getBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String dataUrl = "data:" + contentType + ";base64," + base64;

        // Đảm bảo người có trong danh sách đặc biệt
        if (!greetingService.isSpecialRecipient(name)) {
            greetingService.addSpecialRecipient(name);
        }

        greetingService.setSpecialPhoto(name, dataUrl);

        response.put("success", true);
        response.put("photoPath", dataUrl);
        response.put("message", "Upload ảnh thành công cho " + name);
        return ResponseEntity.ok(response);
    }

    /** Xóa ảnh — set photoPath = null trong DB */
    @PostMapping("/api/admin/photo-remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removePhoto(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Tên không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        greetingService.setSpecialPhoto(name, null);
        response.put("success", true);
        response.put("message", "Đã xóa ảnh của " + name);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/admin/special-add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addSpecial(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Tên không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        greetingService.addSpecialRecipient(name.trim());
        response.put("success", true);
        response.put("message", "Đã thêm " + name.trim());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/admin/special-remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeSpecial(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Tên không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        greetingService.removeSpecialRecipient(name.trim());
        response.put("success", true);
        response.put("message", "Đã xóa " + name.trim());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/admin/special-recipients")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSpecialRecipients() {
        Map<String, Object> response = new HashMap<>();
        List<String> names = greetingService.getSpecialRecipientsList();
        response.put("success", true);
        response.put("specialRecipientsList", names);

        Map<String, Map<String, String>> savedConfigs = new LinkedHashMap<>();
        for (String name : names) {
            greetingService.getConfig(name).ifPresent(cfg -> {
                Map<String, String> info = new HashMap<>();
                info.put("message", cfg.getMessage() != null ? cfg.getMessage() : "");
                info.put("photoPath", cfg.getPhotoPath() != null ? cfg.getPhotoPath() : "");
                savedConfigs.put(name, info);
            });
        }
        response.put("savedConfigs", savedConfigs);
        response.put("customMessages", greetingService.getAllCustomMessages());

        greetingService.getConfig("__general__").ifPresent(cfg ->
                response.put("generalMessage", cfg.getMessage() != null ? cfg.getMessage() : "")
        );

        return ResponseEntity.ok(response);
    }
}