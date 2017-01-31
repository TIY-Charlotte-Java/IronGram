package com.theironyard.controllers;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import com.theironyard.services.PhotoRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utilities.PasswordStorage;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling
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
    public void destroy()  {
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
    public Photo upload(HttpSession session,
                        HttpServletResponse response,
                        String receiver,
                        MultipartFile photo,
                        Integer countDown) throws Exception {

        String username = (String) session.getAttribute("username");

        if (username == null) {
            throw new Exception("Not logged in.");
        }

        User senderUser = users.findFirstByName(username);
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

        Photo p = new Photo();
        p.setSender(senderUser);
        p.setRecipient(receiverUser);
        p.setFilename(photoFile.getName());

        if (countDown == null) {
            p.setCountDown(10); // set countdown to 10
        } else {
            p.setCountDown(countDown); // set countdown to w/e the recipient wants
        }
        photos.save(p);
        photoFile.deleteOnExit();

        response.sendRedirect("/");
        return p;
    }

    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception { // Session is passed as parameter
        String username = (String) session.getAttribute("username"); // saves the username from session
        if (username == null) { // if no username then throw Exception
            throw new Exception("Not logged in."); // everything works but when the site boots up this exception is thrown until someone logs in...
        }
        User user = users.findFirstByName(username); // returns user object:search users repo for current sessions username
        List<Photo> photo = photos.findByRecipient(user); // returns photos by that user

        Photo removePhoto = photos.findPhotoByRecipient(user);
        if (photo != null) {
            new Thread(() -> {
                try {
                    Thread.sleep(removePhoto.getCountDown() * 1000); //is in milliseconds so we multiply by 1000
                    photos.delete(photo);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }).start();

        }
        return photos.findByRecipient(user);
    }
}