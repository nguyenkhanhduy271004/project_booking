package com.booking.booking.service.interfaces;

import com.booking.booking.dto.request.ChangePasswordRequest;
import com.booking.booking.dto.request.ForgetPasswordRequest;
import com.booking.booking.dto.request.UserCreationRequest;
import com.booking.booking.dto.request.UserPasswordRequest;
import com.booking.booking.dto.request.UserUpdateRequest;
import com.booking.booking.dto.response.PageResponse;
import com.booking.booking.dto.response.UserPageResponse;
import com.booking.booking.dto.response.UserResponse;
import java.util.List;

import org.springframework.data.domain.Pageable;

public interface UserService {

  UserPageResponse findAll(String keyword, String sort, int page, int size);

  PageResponse<?> advanceSearchWithSpecification(Pageable pageable, String[] user);

  UserPageResponse findAllUsersIsDeleted(String keyword, String sort, int page, int size);

  UserResponse findById(Long id);

  UserResponse findByUsername(String username);

  UserResponse findByEmail(String email);

  UserPageResponse findUserForManager(Pageable pageable);

  UserPageResponse findUserForStaff(Pageable pageable);

  long save(UserCreationRequest req);

  void update(UserUpdateRequest req);

  void changePassword(UserPasswordRequest req);

  void deleteById(Long id);

  void deleteByIds(List<Long> ids);

  void deletePermanentlyById(Long id);

  void deletePermanentlyByIds(List<Long> ids);

  void confirmEmail(String secretCode);

  void restoreById(Long id);

  void restoreByIds(List<Long> ids);

  void resetPassword(ForgetPasswordRequest req);

  void verifyEmail(String email);

  boolean verifyOtp(Integer otp, String email);

  void resetPassword(String email, ChangePasswordRequest req);

  void activeAccount(String email);
}
