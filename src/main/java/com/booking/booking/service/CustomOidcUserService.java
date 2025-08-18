package com.booking.booking.service;

import com.booking.booking.common.UserType;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.model.Role;
import com.booking.booking.model.User;
import com.booking.booking.model.UserHasRole;
import com.booking.booking.repository.RoleRepository;
import com.booking.booking.repository.UserRepository;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@Slf4j(topic = "Custom-Oidc-User-Service")
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private final OidcUserService oidcUserService;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;


  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    log.info("registration id: {}", registrationId);

    OidcUser oidcUser = this.oidcUserService.loadUser(userRequest);

    String name = oidcUser.getAttribute("name");
    String email = oidcUser.getAttribute("email");

    User user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
      user = new User();
      user.setEmail(email);
      user.setLastName(name);
      user.setType(UserType.GUEST);

      Role role = roleRepository.findByName("GUEST");
      if (role == null) {
        throw new BadRequestException("Invalid role: GUEST");
      }

      UserHasRole userHasRole = new UserHasRole(user, role);
      user.setRoles(new HashSet<>());
      user.getRoles().add(userHasRole);

      userRepository.save(user);
    }

    log.info("name = {}, email: {}", name, email);
    return oidcUser;
  }
}
