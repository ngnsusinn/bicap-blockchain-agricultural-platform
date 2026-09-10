package vn.courses.ut.edu.javaprogramming.bicap.common.security;

import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
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
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCaseOrPhone(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return user;
    }
}
