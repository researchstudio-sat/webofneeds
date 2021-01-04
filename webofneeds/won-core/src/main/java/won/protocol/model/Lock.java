package won.protocol.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "lock", uniqueConstraints = { @UniqueConstraint(name = "IDX_UNIQUE_TABLE", columnNames = "name") })
public class Lock {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;

    public Lock() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
