package com.womenday.app.service;

import com.womenday.app.model.Greeting;
import com.womenday.app.model.GreetingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GreetingService {

    // Key đặc biệt lưu danh sách người đặc biệt trong DB
    private static final String SPECIAL_LIST_KEY = "__special_list__";

    // 3 người mặc định ban đầu
    private static final List<String> DEFAULT_SPECIALS = Arrays.asList(
        "Nguyễn Thị Thúy Hường",
        "Nguyễn Thị Thu Trang",
        "Nguyễn Bảo Ngọc"
    );

    @Autowired
    private GreetingConfigRepository repo;

    /** Lấy danh sách người đặc biệt từ DB (hoặc default nếu chưa có) */
    public List<String> getSpecialRecipientsList() {
        Optional<GreetingConfig> cfg = repo.findById(SPECIAL_LIST_KEY);
        if (cfg.isPresent() && cfg.get().getMessage() != null && !cfg.get().getMessage().isBlank()) {
            return Arrays.asList(cfg.get().getMessage().split("\\|\\|"));
        }
        // Lần đầu: lưu default vào DB
        saveSpecialList(DEFAULT_SPECIALS);
        return new ArrayList<>(DEFAULT_SPECIALS);
    }

    private void saveSpecialList(List<String> names) {
        String joined = String.join("||", names);
        GreetingConfig cfg = repo.findById(SPECIAL_LIST_KEY)
            .orElse(new GreetingConfig(SPECIAL_LIST_KEY, null, null));
        cfg.setMessage(joined);
        repo.save(cfg);
    }

    public boolean isSpecialRecipient(String name) {
        return getSpecialRecipientsList().contains(name.trim());
    }

    /** Thêm người vào danh sách đặc biệt */
    public void addSpecialRecipient(String name) {
        List<String> list = new ArrayList<>(getSpecialRecipientsList());
        if (!list.contains(name.trim())) {
            list.add(name.trim());
            saveSpecialList(list);
        }
    }

    /** Xóa người khỏi danh sách đặc biệt (và xóa config của họ) */
    public void removeSpecialRecipient(String name) {
        List<String> list = new ArrayList<>(getSpecialRecipientsList());
        list.remove(name.trim());
        saveSpecialList(list);
        // Xóa config (lời chúc + ảnh) của người đó
        repo.deleteById(name.trim());
    }

    public Greeting getGreeting(String name) {
        String trimmedName = name.trim();
        boolean isSpecial = isSpecialRecipient(trimmedName);
        Optional<GreetingConfig> config = repo.findById(trimmedName);

        String message;
        if (config.isPresent() && config.get().getMessage() != null && !config.get().getMessage().isBlank()) {
            message = config.get().getMessage();
        } else {
            Optional<GreetingConfig> general = repo.findById("__general__");
            if (general.isPresent() && general.get().getMessage() != null && !general.get().getMessage().isBlank()) {
                message = general.get().getMessage().replace("{name}", trimmedName);
            } else {
                message = buildDefaultMessage(trimmedName, isSpecial);
            }
        }

        String photoPath = null;
        if (isSpecial && config.isPresent()) {
            photoPath = config.get().getPhotoPath();
        }

        return new Greeting(trimmedName, message, isSpecial, photoPath);
    }

    private String buildDefaultMessage(String name, boolean isSpecial) {
        if (isSpecial) {
            return "Nhân ngày Quốc tế Phụ nữ 8/3, kính chúc " + name
                + " luôn tươi trẻ, rạng rỡ và tràn đầy hạnh phúc. "
                + "Chúc bạn luôn thành công trong công việc và cuộc sống, "
                + "mãi mãi tỏa sáng như bông hoa đẹp nhất! 🌹💐";
        }
        return "Chúc mừng ngày Quốc tế Phụ nữ 8/3! Kính chúc " + name
            + " luôn vui vẻ, hạnh phúc và tràn đầy sức khỏe. "
            + "Chúc bạn luôn xinh đẹp, tỏa sáng trong mọi hoàn cảnh! 🌺🌸";
    }

    public void setCustomMessage(String name, String message) {
        String trimmed = name.trim();
        GreetingConfig config = repo.findById(trimmed).orElse(new GreetingConfig(trimmed, null, null));
        config.setMessage(message);
        repo.save(config);
    }

    public void setSpecialPhoto(String name, String photoPath) {
        String trimmed = name.trim();
        GreetingConfig config = repo.findById(trimmed).orElse(new GreetingConfig(trimmed, null, null));
        config.setPhotoPath(photoPath);
        repo.save(config);
    }

    public Map<String, String> getAllCustomMessages() {
        Map<String, String> result = new LinkedHashMap<>();
        repo.findAll().forEach(c -> {
            if (c.getMessage() != null && !c.getMessage().isBlank()
                && !c.getRecipientName().startsWith("__")) {
                result.put(c.getRecipientName(), c.getMessage());
            }
        });
        return result;
    }

    public Optional<GreetingConfig> getConfig(String name) {
        return repo.findById(name.trim());
    }
}
