package com.fran.users_service.app.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fran.users_service.app.dtos.UserDTO;
import com.fran.users_service.app.models.User;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT new com.fran.users_service.app.dtos.UserDTO(u.id, u.name, u.email, u.phone) FROM User u WHERE u.email = :email")
    Optional<UserDTO> findUserDTOByEmail(@Param("email") String email);

    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);
}
