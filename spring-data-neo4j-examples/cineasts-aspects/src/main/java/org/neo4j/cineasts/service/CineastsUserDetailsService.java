package org.neo4j.cineasts.service;

import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.User;
import org.neo4j.cineasts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mh
 * @since 06.03.11
 */
@Service
public class CineastsUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException, DataAccessException {
        final User user = findUser(login);
        if (user==null) throw new UsernameNotFoundException("Username not found",login);
        return new CineastsUserDetails(user);
    }

    public User findUser(String login) {
        return userRepository.findByPropertyValue("login",login);
    }


    public User getUserFromSession() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof CineastsUserDetails) {
            CineastsUserDetails userDetails = (CineastsUserDetails) principal;
            return userDetails.getUser();
        }
        return null;
    }

    @Transactional
    public User register(String login, String name, String password) {
        User found = findUser(login);
        if (found!=null) throw new RuntimeException("Login already taken: "+login);
        if (name==null || name.isEmpty()) throw new RuntimeException("No name provided.");
        if (password==null || password.isEmpty()) throw new RuntimeException("No password provided.");
        User user=userRepository.save(new User(login,name,password,User.Roles.ROLE_USER));
        setUserInSession(user);
        return user;
    }

    private void setUserInSession(User user) {
        SecurityContext context = SecurityContextHolder.getContext();
        CineastsUserDetails userDetails = new CineastsUserDetails(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, user.getPassword(),userDetails.getAuthorities());
        context.setAuthentication(authentication);

    }

    @Transactional
    public void addFriend(String login) {
        User friend = findUser(login);
        User user = getUserFromSession();
        if (!user.equals(friend)) {
            user.addFriend(friend);
        }
    }

    @Transactional
    public void rate(User user, Movie movie, Integer stars, String comment) {
        if (user == null || movie == null) return;
        int stars1 = stars==null ? -1 : stars;
        String comment1 = comment!=null ? comment.trim() : null;
        user.rate(movie, stars1, comment1);
    }
}
