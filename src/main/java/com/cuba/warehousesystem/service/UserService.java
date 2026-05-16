package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.UserCreateRequest;
import com.cuba.warehousesystem.dto.UserResponse;
import com.cuba.warehousesystem.dto.UserUpdateRequest;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(UserCreateRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(existing -> {
            throw new BadRequestException("User with username already exists: " + request.username());
        });

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setIsActive(request.isActive() == null || request.isActive());
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return userRepository.search(search.trim(), pageable).map(this::toResponse);
        }
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setIsActive(request.isActive() == null || request.isActive());
        return toResponse(userRepository.save(user));
    }

    public void delete(Long id) {
        User user = findUser(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
