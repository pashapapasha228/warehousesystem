package com.cuba.warehousesystem.repository;

import com.cuba.warehousesystem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("""
            select u from User u
            where lower(u.username) like lower(concat('%', :search, '%'))
               or lower(coalesce(u.fullName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
            """)
    Page<User> search(@Param("search") String search, Pageable pageable);
}
