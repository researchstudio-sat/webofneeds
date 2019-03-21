package won.owner.model;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * Entity that holds a verificationToken for Users so we enable the EmailVerification Process
 */
@Entity @Table(name = "verificationtoken") public class EmailVerificationToken {
  private static final int EXPIRATION = 60 * 24; //Token will expire after a day
  private static final TokenPurpose DEFAULT_PURPOSE = TokenPurpose.INITIAL_EMAIL_VERIFICATION;

  @Id @GeneratedValue private Long id;

  private String token;

  @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER) @JoinColumn(nullable = false, name = "user_id") private User user;

  @Column(name = "purpose") @Enumerated(EnumType.STRING) private TokenPurpose purpose;

  private Date expiryDate;

  public EmailVerificationToken() {
  }

  public EmailVerificationToken(User user, String token, Date expiryDate, TokenPurpose purpose) {
    this.user = user;
    this.token = token;
    this.expiryDate = expiryDate;
    this.purpose = purpose;
  }

  public EmailVerificationToken(User user, String token) {
    this.user = user;
    this.token = token;
    this.expiryDate = calculateExpiryDate(EXPIRATION);
    this.purpose = TokenPurpose.INITIAL_EMAIL_VERIFICATION;
  }

  private Date calculateExpiryDate(int expiryTimeInMinutes) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Timestamp(cal.getTime().getTime()));
    cal.add(Calendar.MINUTE, expiryTimeInMinutes);
    return new Date(cal.getTime().getTime());
  }

  //Getter & Setter
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Date getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(Date expiryDate) {
    this.expiryDate = expiryDate;
  }

  public void setPurpose(TokenPurpose purpose) {
    this.purpose = purpose;
  }

  public TokenPurpose getPurpose() {
    return purpose;
  }

  /**
   * Method that checks if the token is expired based on the current datetime
   *
   * @return true if the token is expired, false if it is still valid
   */
  public boolean isExpired() {
    return isExpired(Calendar.getInstance());
  }

  /**
   * Method that checks if the token is expired based on the given datetime
   *
   * @param cal date to check the expiryDate with
   * @return true if the token is expired, false if it is still valid
   */
  public boolean isExpired(Calendar cal) {
    return (this.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0;
  }
}
