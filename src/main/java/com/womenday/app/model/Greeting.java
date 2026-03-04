package com.womenday.app.model;

public class Greeting {
    private String recipientName;
    private String message;
    private boolean hasPhoto;
    private String photoPath;

    public Greeting() {}

    public Greeting(String recipientName, String message, boolean hasPhoto, String photoPath) {
        this.recipientName = recipientName;
        this.message = message;
        this.hasPhoto = hasPhoto;
        this.photoPath = photoPath;
    }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isHasPhoto() { return hasPhoto; }
    public void setHasPhoto(boolean hasPhoto) { this.hasPhoto = hasPhoto; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
}
