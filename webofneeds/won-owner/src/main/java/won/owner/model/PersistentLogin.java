package won.owner.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "persistent_logins")
public class PersistentLogin {

  @Column(name = "username")
  private String username;

  @Column(name = "series")
  @Id
  private String series;

  @Column(name = "token")
  private String token;

  @Column(name = "last_used")
  private Date lastUsed;

  @JoinColumn(name = "keystore_password_id")
  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
  private KeystorePasswordHolder keystorePasswordHolder;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getSeries() {
    return series;
  }

  public void setSeries(String series) {
    this.series = series;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Date getLastUsed() {
    return lastUsed;
  }

  public void setLastUsed(Date lastUsed) {
    this.lastUsed = lastUsed;
  }

  public KeystorePasswordHolder getKeystorePasswordHolder() {
    return keystorePasswordHolder;
  }

  public void setKeystorePasswordHolder(KeystorePasswordHolder keystorePasswordHolder) {
    this.keystorePasswordHolder = keystorePasswordHolder;
  }

}
