package com.theironyard.services;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PhotoRepository extends CrudRepository<Photo, Integer> {
    List<Photo> findByRecipient(User receiver);
    Photo findPhotoByRecipient(User receiver);
}
