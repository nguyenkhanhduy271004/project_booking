package com.booking.booking.service;

import com.booking.booking.model.ForgotPassword;
import com.booking.booking.repository.ForgotPasswordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForgotPasswordTxService {
  private final ForgotPasswordRepository forgotPasswordRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void delete(ForgotPassword fp) {
    forgotPasswordRepository.delete(fp);
  }
}
