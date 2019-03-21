package won.owner.model;

import won.owner.service.impl.KeystorePasswordUtils;

import javax.persistence.*;

@Entity @Table(name = "keystore_password") public class KeystorePasswordHolder {
  @Id @GeneratedValue @Column(name = "id") private Long id;

  @Column(name = "encrypted_password") private String encryptedPassword;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEncryptedPassword() {
    return encryptedPassword;
  }

  public void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  public String getPassword(String key) {
    return KeystorePasswordUtils.decryptPassword(encryptedPassword, key);
  }

  public void setPassword(String cleartextPassword, String key) {
    this.encryptedPassword = KeystorePasswordUtils.encryptPassword(cleartextPassword, key);
  }

}
