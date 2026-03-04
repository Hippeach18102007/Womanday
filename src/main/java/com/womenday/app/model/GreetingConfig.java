package com.womenday.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "greeting_config")
public class GreetingConfig {

    @Id
    @Column(name = "recipient_name", length = 200, columnDefinition = "nvarchar(200)")
    private String recipientName;

    @Column(name = "message", length = 2000, columnDefinition = "nvarchar(2000)")
    private String message;

    @Column(name = "photo_path", length = 500, columnDefinition = "nvarchar(500)")
    private String photoPath;

    public GreetingConfig() {}

    public GreetingConfig(String recipientName, String message, String photoPath) {
        this.recipientName = recipientName;
        this.message = message;
        this.photoPath = photoPath;
    }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
}
