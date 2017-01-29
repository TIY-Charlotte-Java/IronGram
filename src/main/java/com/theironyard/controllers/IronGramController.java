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
import java.io.*;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

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
            String receiver,
            MultipartFile photo,
            int time
    ) throws Exception {
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
        //.createTempFile -> prefixes file name with "photo" + original file name + random string of integers
        //gives us a unique file name
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());
        photoFile.deleteOnExit();//deletes file once program stops running

        Photo p = new Photo();
        p.setSender(senderUser);
        p.setRecipient(receiverUser);
        p.setFilename(photoFile.getName());
        p.setTime(time);
        photos.save(p);

        response.sendRedirect("/");

        return p;
    }

    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {//Session is passed as param
        String username = (String) session.getAttribute("username");//saves the username from session
        if (username == null) {//throws exception if no username is found
            throw new Exception("Not logged in.");
        }
        //sending up new thread and executing after a certain amount of time
        //before starting a new thread, you have to specify the code to be executed by this thread (by implementing Runnable)
        User user = users.findFirstByName(username);
            List <Photo> userPhotos = photos.findByRecipient(user)//finds photos
            .stream().collect(Collectors.toList());//stores all those photos in a List
        if (userPhotos != null) {
            Photo photo = photos.findFirstPhotoByRecipient(user);
            final int seconds = photo.getTime() * 1000;//time is set to milliseconds by default.
            // Multiplying by 1000 so that thread sleeps for that number of seconds
            new Thread(() -> {
                try {

                    Thread.sleep(seconds);
                    photos.delete(userPhotos);//deletes everything stored for that user in photos repository
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        return photos.findByRecipient(user);
    }
}