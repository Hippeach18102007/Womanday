package com.womenday.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.womenday.app.model.GreetingConfig;
import com.womenday.app.service.GreetingConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class PlaylistController {

    private static final String PLAYLIST_KEY = "__playlist__";
    private static final String PLAYMODE_KEY = "__playmode__";

    @Autowired
    private GreetingConfigRepository repo;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/api/admin/playlist")
    public ResponseEntity<Map<String, Object>> getPlaylist() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Load playlist JSON
            List<Map<String, Object>> playlist = new ArrayList<>();
            Optional<GreetingConfig> cfg = repo.findById(PLAYLIST_KEY);
            if (cfg.isPresent() && cfg.get().getMessage() != null && !cfg.get().getMessage().isBlank()) {
                playlist = mapper.readValue(cfg.get().getMessage(), List.class);
            }

            // Load play mode
            String playMode = "next";
            Optional<GreetingConfig> modeCfg = repo.findById(PLAYMODE_KEY);
            if (modeCfg.isPresent() && modeCfg.get().getMessage() != null) {
                playMode = modeCfg.get().getMessage();
            }

            response.put("success", true);
            response.put("playlist", playlist);
            response.put("playMode", playMode);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/admin/playlist")
    public ResponseEntity<Map<String, Object>> savePlaylist(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Object playlistObj = body.get("playlist");
            String playMode = String.valueOf(body.getOrDefault("playMode", "next"));

            // Lưu playlist dạng JSON string
            String json = mapper.writeValueAsString(playlistObj);
            GreetingConfig cfg = repo.findById(PLAYLIST_KEY)
                    .orElse(new GreetingConfig(PLAYLIST_KEY, null, null));
            cfg.setMessage(json);
            repo.save(cfg);

            // Lưu play mode
            GreetingConfig modeCfg = repo.findById(PLAYMODE_KEY)
                    .orElse(new GreetingConfig(PLAYMODE_KEY, null, null));
            modeCfg.setMessage(playMode);
            repo.save(modeCfg);

            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}
