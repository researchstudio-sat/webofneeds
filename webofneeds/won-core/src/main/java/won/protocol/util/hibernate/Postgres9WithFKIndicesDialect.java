package won.protocol.util.hibernate;

import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * Hibernate dialect for postgres 9 that creates indices for foreign keys automatically
 *
 * Created by hfriedrich on 17.10.2015.
 */
public class Postgres9WithFKIndicesDialect extends PostgreSQL9Dialect
{
  public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKey, String referencedTable, String[] primaryKey, boolean referencesPrimaryKey) {
    String cols = StringHelper.join(", ", foreignKey);
    String referencedCols = StringHelper.join(", ", primaryKey);
    return String.format(" add constraint %s foreign key (%s) references %s (%s)", new Object[]{constraintName, cols, referencedTable, referencedCols});
  }
}
