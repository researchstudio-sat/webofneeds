package won.protocol;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 04.11.12
 * Time: 17:11
 * To change this template use File | Settings | File Templates.
 */
public interface UpdateEntity<M> {
    public void update(M Entity);
    public Long getId(M Entity);
}
