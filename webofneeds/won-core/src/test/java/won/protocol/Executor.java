package won.protocol;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.repository.*;

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

@ContextConfiguration("/applicationContext.xml")
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
@Component
public class Executor {

    @Autowired
    private NeedRepository needRepository;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    public void execute() {
        final List<Need> needs = new ArrayList<Need>();
        try {
            final Need test123 = new Need();
            test123.setNeedURI(new URI("test123"));
            needs.add(test123);

            final Need test456 = new Need();
            test456.setNeedURI(new URI("test456"));
            needs.add(test456);
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println("before insert (no id):");
        for (final Need n : needs) {
            System.out.format("  * %s\n", n);
        }
        System.out.println();
        test(needRepository, needs, new UpdateEntity<Need>() {
            @Override
            public void update(Need Entity) {
                try {
                    Entity.setOwnerURI(new URI("test789"));
                } catch (URISyntaxException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            @Override
            public Long getId(Need Entity) {
                return Entity.getId();
            }
        });

        final List<Match> matches = new ArrayList<Match>();
        final Match m1 = new Match();
        matches.add(m1);
        final Match m2 = new Match();
        matches.add(m2);

        test(matchRepository, matches, new UpdateEntity<Match>() {
            @Override
            public void update(Match Entity) {
                Entity.setScore(2);
            }

            @Override
            public Long getId(Match Entity) {
                return Entity.getEventId();
            }
        });

        final List<Connection> connections = new ArrayList<Connection>();
        final Connection c1 = new Connection();
        connections.add(c1);
        final Connection c2 = new Connection();
        connections.add(c2);

        test(connectionRepository, connections, new UpdateEntity<Connection>() {
            @Override
            public void update(Connection Entity) {
                try {
                    Entity.setNeedURI(new URI("test123"));
                } catch (URISyntaxException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            @Override
            public Long getId(Connection Entity) {
                return Entity.getId();  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        final List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
        final ChatMessage cm1 = new ChatMessage();
        chatMessages.add(cm1);
        final ChatMessage cm2 = new ChatMessage();
        chatMessages.add(cm2);

        test(chatMessageRepository, chatMessages, new UpdateEntity<ChatMessage>() {
            @Override
            public void update(ChatMessage Entity) {
                Entity.setMessage("hi");
            }

            @Override
            public Long getId(ChatMessage Entity) {
                return Entity.getId();
            }
        });
    }

    public<M> void test(WonRepository<M> rep, List<M> l, UpdateEntity<M> u) {
        // create users

        try {
            // persist users
            rep.save(l);
            System.out.println("after insert (with id):");
            for (final M n : l) {
                System.out.format("  * %s\n", n);
            }
            System.out.println();

            // find users by username
            System.out.format("users with username like test123\n");
            final List<M> founds = rep.findById(1l);
            for (final M m : founds) {
                System.out.format("  * %s\n", m);
            }
            System.out.println();

            // update user
            if (!founds.isEmpty()) {
                    final M test = founds.get(0);
                    u.update(test);
                    rep.save(test);

                    final M updatedNeed = rep.findOne(u.getId(test));
                    System.out.format(" after update:\n  * %s\n\n", updatedNeed);
            }

            // get all users from db
            System.out.println("all users:");
            for (final M n : rep.findAll()) {
                System.out.format("  * %s\n", n);
            }
            System.out.println();
        } finally {
            // delete all users from db
            System.out.format("count before deletion: %s\n", rep.count());
            rep.deleteAll();
            System.out.format(" count after deletion: %s", rep.count());
        }
    }

}