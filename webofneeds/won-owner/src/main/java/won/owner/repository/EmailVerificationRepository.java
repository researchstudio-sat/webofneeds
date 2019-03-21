package won.owner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import won.owner.model.EmailVerificationToken;
import won.owner.model.User;

import java.util.List;

/**
 * Created by fsuda on 27.11.2018.
 */
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationToken, Long> {
  EmailVerificationToken findByToken(String token);

  List<EmailVerificationToken> findByUser(User user);
}
