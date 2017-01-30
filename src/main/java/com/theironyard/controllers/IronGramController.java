package com.theironyard.controllers;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import com.theironyard.services.PhotoRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utilities.PasswordStorage;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@RestController
public class IronGramController {
    @Autowired
    UserRepository users;

    @Autowired
    PhotoRepository photos;

    Server dbui = null;

    @PostConstruct
    public void init() throws SQLException {
        dbui = Server.createWebServer().start();
    }

    @PreDestroy
    public void destroy() {
        dbui.stop();
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public User login(String username, String password, HttpSession session, HttpServletResponse response) throws Exception {
        User user = users.findFirstByName(username);

        if (user == null) {
            user = new User(username, PasswordStorage.createHash(password));
            users.save(user);
        }

        else if (!PasswordStorage.verifyPassword(password, user.getPassword())) {
            throw new Exception("Wrong password");
        }

        session.setAttribute("username", username);
        response.sendRedirect("/");
        return user;
    }

    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate();
        response.sendRedirect("/");
    }

    @RequestMapping(path = "/user", method = RequestMethod.GET)
    public User getUser(HttpSession session) {
        String username = (String) session.getAttribute("username");
        return users.findFirstByName(username);
    }


    @RequestMapping("/upload")
    public Photo upload(
            HttpSession session,
            HttpServletResponse response,
            String receiver, int seconds,
            MultipartFile photo // parameters for uploading the pictures
    ) throws Exception {
        String username = (String) session.getAttribute("username");

        if (username == null) { // user name is null then through that the person is not logged in
            throw new Exception("Not logged in.");
        }

        User senderUser = users.findFirstByName(username); //sender is assigned by sql method
        User receiverUser = users.findFirstByName(receiver);

        if (receiverUser == null) {
            throw new Exception("Receiver name doesn't exist.");
        }

        if (!photo.getContentType().startsWith("image")) {
            throw new Exception("Only images are allowed.");
        }

        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), new File("build/resources/main/static"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        //creates a new photo and sets the parameters
        Photo p = new Photo();
        p.setSender(senderUser);
        p.setRecipient(receiverUser);
        p.setFilename(photoFile.getName());
        p.setTimer(seconds);
        photos.save(p);
        photoFile.deleteOnExit();//deleted when the virtual machine terminates


        response.sendRedirect("/");

        return p;
    }


    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {// session is passed as parameter
        String username = (String) session.getAttribute("username"); // save the user name from the session
        if (username == null) {// if no username then throw exception
            throw new Exception("Not logged in.");
        }
        User user = users.findFirstByName(username); // finds a user
        List<Photo> photo = photos.findByRecipient(user); // finds a photo by the recipient

        Photo newphoto = photos.findFirstPhotoByRecipient(user);
        final int millSec = 1000; // assignment for milloseconds
        int seconds = newphoto.getTimer() * millSec; // convert users time into milliseconds

        new Thread(() -> { // creates a thread
            while(true){
                try {
                    Thread.sleep(seconds); // sleeps the thread after a given time
                    photos.delete(photo); // deletes the photo from the file
                    return;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return photo;


    }


}