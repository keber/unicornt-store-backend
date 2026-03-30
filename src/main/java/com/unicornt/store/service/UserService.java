package com.unicornt.store.service;

import com.unicornt.store.dto.RegisterRequest;
import com.unicornt.store.model.User;

public interface UserService {

    User register(RegisterRequest request);

    boolean emailExists(String email);
}
