package com.loanmust.loan.security;

import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Accept either the email address or the optional username.
        User user = userRepository.findByEmailAndDeletedFalse(login)
                .or(() -> userRepository.findByUsernameAndDeletedFalse(login))
                .orElseThrow(() -> new UsernameNotFoundException("No account found for " + login));
        return new CustomUserDetails(user);
    }
}
