package won.protocol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.model.Need;
import won.protocol.repository.NeedRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 02.11.12
 * Time: 15:59
 * To change this template use File | Settings | File Templates.
 */

@Component
public class Executor {

    @Autowired
    private NeedRepository needRepository;

    public void execute() {

        // create users
        final List<Need> needs = new ArrayList<Need>();

        try {
            final Need test123 = new Need();
            test123.setNeedURI(new URI("test123"));
            needs.add(test123);

            final Need test456 = new Need();
                test456.setNeedURI(new URI("test456"));
            needs.add(test456);


            System.out.println("before insert (no id):");
            for (final Need n : needs) {
                System.out.format("  ● %s\n", n);
            }
            System.out.println();

            // persist users
            needRepository.save(needs);
            System.out.println("after insert (with id):");
            for (final Need n : needs) {
                System.out.format("  ● %s\n", n);
            }
            System.out.println();

            // find users by username
            System.out.format("users with username like test123\n");
            final List<Need> foundNeeds = needRepository.findByNeedURI(new URI("test123"));
            for (final Need u : foundNeeds) {
                System.out.format("  ● %s\n", u);
            }
            System.out.println();

            // update user
            if (!foundNeeds.isEmpty()) {
                try {
                    final Need test789 = foundNeeds.get(0);
                        test789.setOwnerURI(new URI("test789"));
                    needRepository.save(test789);

                    final Need updatedNeed = needRepository.findOne(test789.getId());
                    System.out.format(" after update:\n  ● %s\n\n", updatedNeed);
                } catch (URISyntaxException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            // get all users from db
            System.out.println("all users:");
            for (final Need n : needRepository.findAll()) {
                System.out.format("  ● %s\n", n);
            }
            System.out.println();
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            // delete all users from db
            System.out.format("count before deletion: %s\n", needRepository.count());
            needRepository.deleteAll();
            System.out.format(" count after deletion: %s", needRepository.count());
        }
    }

}