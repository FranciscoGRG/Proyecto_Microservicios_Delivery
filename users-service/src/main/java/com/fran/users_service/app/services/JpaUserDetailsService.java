package com.fran.users_service.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fran.users_service.app.repositories.IUserRepository;

@Service
public class JpaUserDetailsService implements UserDetailsService {
        @Autowired
    private IUserRepository repository;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmail(email)
                         .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
