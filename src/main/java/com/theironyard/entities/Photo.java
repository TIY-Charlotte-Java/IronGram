package com.theironyard.entities;

import javax.persistence.*;

@Entity
@Table(name = "photos")
public class Photo {
    @Id
    @GeneratedValue
    int id;

    @ManyToOne
    User sender;

    @ManyToOne
    User recipient;

    @Column(nullable = false)
    String filename;

    //keep track of time
    @Column(nullable = false)
    int timer;

    // creating columns for public access
    @Column(nullable = false)
    boolean access;


    //add a column for add how many timer they want the photo to exist

    public Photo() {
    }

    public Photo(User sender, User recipient, String filename, int timer) {
        this.sender = sender;
        this.recipient = recipient;
        this.filename = filename;
        this.timer = timer;
    }

    public Photo(User sender, User recipient, String filename, int timer, boolean access) {
        this.sender = sender;
        this.recipient = recipient;
        this.filename = filename;
        this.timer = timer;
        this.access = access;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getTimer() {
        return timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }

    public boolean isAccess() {
        return access;
    }

    public void setAccess(boolean access) {
        this.access = access;
    }
}