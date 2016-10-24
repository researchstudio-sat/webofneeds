package won.bot.framework.bot.context.repo;

import org.springframework.data.repository.CrudRepository;

/**
 * Created by hfriedrich on 24.10.2016.
 */
public interface ObjectRepo extends CrudRepository<MyObject, String>
{
}
