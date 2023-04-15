package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.user.UserRepository;

public class UserManager {

    private final UserRepository repository;

    public UserManager(UserRepository repository) {
        this.repository = repository;
    }

}
