package com.womenday.app.service;

import com.womenday.app.model.Greeting;
import com.womenday.app.model.GreetingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GreetingService {

    private static final String SPECIAL_LIST_KEY = "__special_list__";

    private static final List<String> DEFAULT_SPECIALS = Arrays.asList(
            "Nguyễn Thị Thúy Hường",
            "Nguyễn Thị Thu Trang",
            "Nguyễn Bảo Ngọc"
    );

    @Autowired
    private GreetingConfigRepository repo;

    // ─── Normalize tên ────────────────────────────────────────
    /**
     * Chuẩn hóa tên: bỏ dấu cách thừa, rồi so khớp không phân biệt
     * hoa/thường và dấu tiếng Việt.
     * "NGUYỄN THỊ THÚY HƯỜNG" → "nguyen thi thuy huong" (để lookup)
     */
    private String normalizeForLookup(String name) {
        if (name == null) return "";
        String lower = name.trim().toLowerCase();
        // Bỏ dấu tiếng Việt (NFD rồi xóa combining marks)
        String nfd = Normalizer.normalize(lower, Normalizer.Form.NFD);
        String noAccent = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[đĐ]", "d")
                .replaceAll("\\s+", " ");
        return noAccent;
    }

    /**
     * Tìm tên thực sự trong danh sách bằng cách so sánh normalized.
     * Trả về tên gốc (đúng hoa/thường/dấu) nếu tìm thấy, hoặc null.
     */
    public String findCanonicalName(String inputName) {
        String inputNorm = normalizeForLookup(inputName);
        for (String stored : getSpecialRecipientsList()) {
            if (normalizeForLookup(stored).equals(inputNorm)) {
                return stored;
            }
        }
        // Cũng thử tìm trong tất cả config (người có lời chúc riêng)
        for (GreetingConfig cfg : repo.findAll()) {
            String storedName = cfg.getRecipientName();
            if (!storedName.startsWith("__") &&
                    normalizeForLookup(storedName).equals(inputNorm)) {
                return storedName;
            }
        }
        return null;
    }

    // ─── Special list ─────────────────────────────────────────
    public List<String> getSpecialRecipientsList() {
        Optional<GreetingConfig> cfg = repo.findById(SPECIAL_LIST_KEY);
        if (cfg.isPresent() && cfg.get().getMessage() != null && !cfg.get().getMessage().isBlank()) {
            return Arrays.asList(cfg.get().getMessage().split("\\|\\|"));
        }
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
        String canonical = findCanonicalName(name);
        return canonical != null && getSpecialRecipientsList().contains(canonical);
    }

    public void addSpecialRecipient(String name) {
        List<String> list = new ArrayList<>(getSpecialRecipientsList());
        String trimmed = name.trim();
        // Kiểm tra trùng (bao gồm cả trùng normalize)
        boolean exists = list.stream()
                .anyMatch(n -> normalizeForLookup(n).equals(normalizeForLookup(trimmed)));
        if (!exists) {
            list.add(trimmed);
            saveSpecialList(list);
        }
    }

    public void removeSpecialRecipient(String name) {
        String canonical = findCanonicalName(name);
        String toRemove = canonical != null ? canonical : name.trim();
        List<String> list = new ArrayList<>(getSpecialRecipientsList());
        list.remove(toRemove);
        saveSpecialList(list);
        repo.deleteById(toRemove);
    }

    // ─── Greeting ─────────────────────────────────────────────
    public Greeting getGreeting(String name) {
        // Tìm tên chuẩn (đúng dấu/hoa thường) từ input bất kỳ
        String canonical = findCanonicalName(name);
        String resolvedName = canonical != null ? canonical : name.trim();

        boolean isSpecial = canonical != null;
        Optional<GreetingConfig> config = repo.findById(resolvedName);

        String message;
        if (config.isPresent() && config.get().getMessage() != null && !config.get().getMessage().isBlank()) {
            message = config.get().getMessage();
        } else {
            // Thử tìm bằng tên gốc nếu canonical khác
            Optional<GreetingConfig> generalCfg = repo.findById("__general__");
            if (generalCfg.isPresent() && generalCfg.get().getMessage() != null
                    && !generalCfg.get().getMessage().isBlank()) {
                message = generalCfg.get().getMessage().replace("{name}", resolvedName);
            } else {
                message = buildDefaultMessage(resolvedName, isSpecial);
            }
        }

        String photoPath = null;
        if (isSpecial && config.isPresent()) {
            photoPath = config.get().getPhotoPath();
        }

        return new Greeting(resolvedName, message, isSpecial, photoPath);
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

    // ─── CRUD ─────────────────────────────────────────────────
    public void setCustomMessage(String name, String message) {
        String canonical = findCanonicalName(name);
        String key = canonical != null ? canonical : name.trim();
        GreetingConfig config = repo.findById(key).orElse(new GreetingConfig(key, null, null));
        config.setMessage(message);
        repo.save(config);
    }

    public void setSpecialPhoto(String name, String photoPath) {
        String canonical = findCanonicalName(name);
        String key = canonical != null ? canonical : name.trim();
        GreetingConfig config = repo.findById(key).orElse(new GreetingConfig(key, null, null));
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
        String canonical = findCanonicalName(name);
        String key = canonical != null ? canonical : name.trim();
        return repo.findById(key);
    }
}