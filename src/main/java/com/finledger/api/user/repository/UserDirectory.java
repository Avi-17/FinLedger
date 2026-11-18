package com.finledger.api.user.repository;

import com.finledger.api.common.NotFoundException;
import com.finledger.api.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Single place for user lookups other modules need (by id, by email, batch name lookup). */
@Service
@RequiredArgsConstructor
public class UserDirectory {
    private final UserRepository users;

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User getByEmail(String email) {
        return users.findByEmail(normalise(email))
                .orElseThrow(() -> new NotFoundException("No user with email " + email));
    }

    /** One query for many users, keyed by id. */
    public Map<Long, User> byIds(Collection<Long> ids) {
        return users.findAllById(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public Map<Long, String> namesByIds(Collection<Long> ids) {
        return users.findAllById(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    public static String normalise(String email) { return email.trim().toLowerCase(); }
}
