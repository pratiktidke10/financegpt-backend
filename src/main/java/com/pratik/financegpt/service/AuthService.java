package com.pratik.financegpt.service;

import com.pratik.financegpt.config.JwtUtil;
import com.pratik.financegpt.entity.User;
import com.pratik.financegpt.model.AuthRequest;
import com.pratik.financegpt.model.AuthResponse;
import com.pratik.financegpt.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(AuthRequest request){
        if(userRepository.existsByUsername(request.getUsername())){
            return new AuthResponse(null , null , "Username already exists");
        }

        if(userRepository.existsByEmail(request.getEmail())){
            return new AuthResponse(null,null,"Email already exists");
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail()
        );

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(token , user.getUsername() , "Registration successful!");
    }

    public AuthResponse login(AuthRequest request){
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            ));

            String token = jwtUtil.generateToken(request.getUsername());

            return new AuthResponse(token , request.getUsername(), "Login Successful!");
        } catch (Exception e) {
            return new AuthResponse(null, null, "Invalid username or password!");
        }
    }
}
