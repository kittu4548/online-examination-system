package com.example.exam.service;

import com.example.exam.model.User;

public interface UserService {
    void saveStudent(User user);
    User findByUsername(String username);
}
