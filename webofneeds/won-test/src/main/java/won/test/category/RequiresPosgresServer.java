package won.test.category;

/**
 * Marker interface for tests that cannot run without a posgreSQL server.
 * Specifically, HSQL does not work for these tests.
 * 
 * @author fkleedorfer
 */
public interface RequiresPosgresServer {
}
