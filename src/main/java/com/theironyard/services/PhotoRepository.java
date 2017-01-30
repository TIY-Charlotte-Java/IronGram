package com.theironyard.services;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PhotoRepository extends CrudRepository<Photo, Integer> {
    // create a list of photos by the recipient
    List<Photo> findByRecipient(User receiver);
    //go to the phot call and find the first photo by the recipient
    Photo findFirstPhotoByRecipient(User receiver);
}
