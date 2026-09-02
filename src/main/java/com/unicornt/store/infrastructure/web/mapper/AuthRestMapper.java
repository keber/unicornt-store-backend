package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.domain.model.User;
import com.unicornt.store.infrastructure.web.dto.AuthDtos.MeResponse;

import java.util.List;

/** Translation between the authentication transport records and the {@link User} model. */
public final class AuthRestMapper {

    private AuthRestMapper() {
    }

    public static MeResponse toMeResponse(User user) {
        List<String> roles = user.roles().stream().sorted().toList();
        return new MeResponse(user.id(), user.firstName(), user.lastName(), user.email(), roles);
    }
}
