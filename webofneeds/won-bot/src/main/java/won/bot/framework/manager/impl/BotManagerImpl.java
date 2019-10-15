package won.bot.framework.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.exception.NoBotResponsibleException;
import won.bot.framework.bot.Bot;
import won.bot.framework.manager.BotManager;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;

/**
 * BotManager, simple in-memory implementation.
 */
public class BotManagerImpl implements BotManager {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private List<Bot> bots = new LinkedList<>(); // presumably a list of all bots managed on this server
    private Map<URI, Bot> botByAtomUri = new HashMap<>(); // map of all bot atoms uris and responsible bots
    private Map<URI, List<Bot>> botListByNodeUri = new HashMap<>(); // map of all bots registered on that node
    private Object monitor = new Object(); // ???

    @Override
    public Bot getBotResponsibleForAtomUri(URI atomUri) throws NoBotResponsibleException {
        // try the botByUri map
        {
            Bot bot = botByAtomUri.get(atomUri);
            if (bot != null) {
                if (!bot.getLifecyclePhase().isActive()) {
                    throw new NoBotResponsibleException("bot responsible for atom " + atomUri
                                    + " is not active (lifecycle phase is: " + bot.getLifecyclePhase() + ")");
                }
                return bot;
            }
        }
        // check each bot, return first that knows the atomUri
        logger.trace("bots size:{} ", bots.size());
        for (Bot mybot : bots) {
            // logger.debug("bot knows atom: {}", mybot.knowsAtomURI(atomUri));
            if (mybot.knowsAtomURI(atomUri)) {
                synchronized (getMonitor()) {
                    this.botByAtomUri.put(atomUri, mybot);
                }
                if (!mybot.getLifecyclePhase().isActive()) {
                    throw new NoBotResponsibleException("bot responsible for atom " + atomUri
                                    + " is not active (lifecycle phase is: " + mybot.getLifecyclePhase() + ")");
                }
                return mybot;
            }
        }
        throw new NoBotResponsibleException("No bot registered for uri " + atomUri);
    }

    @Override
    public List<Bot> getBotsForNodeURI(URI wonNodeUri) {
        {
            List<Bot> botList = botListByNodeUri.get(wonNodeUri);
            if (botList != null && botList.size() > 0)
                return botList;
        }
        List<Bot> botList = new ArrayList<>();
        for (Bot mybot : bots) {
            if (mybot.knowsNodeURI(wonNodeUri)) {
                synchronized (getMonitor()) {
                    botList.add(mybot);
                }
            }
        }
        this.botListByNodeUri.put(wonNodeUri, botList);
        return botList;
    }

    @Override
    public void addBot(Bot bot) {
        synchronized (getMonitor()) {
            if (bots.contains(bot))
                return;
            initializeBotIfNecessary(bot);
            this.bots.add(bot);
        }
    }

    @Override
    public void setBots(Collection<Bot> bots) {
        synchronized (getMonitor()) {
            this.bots.clear();
            this.bots.addAll(bots);
            this.botByAtomUri.clear();
        }
    }

    @Override
    public boolean isWorkDone() {
        logger.debug("checking if the bots' work is all done");
        synchronized (getMonitor()) {
            for (Bot bot : getBots()) {
                if (!bot.isWorkDone()) {
                    logger.debug("bot {} is not done yet", bot);
                    return false;
                }
            }
        }
        logger.debug("all bots are done");
        return true;
    }

    protected void initializeBotIfNecessary(Bot bot) {
        if (bot.getLifecyclePhase().isDown()) {
            try {
                logger.info("initializing bot {}", bot);
                bot.initialize();
            } catch (Exception e) {
                logger.warn("could not initialize bot {} ", bot, e);
            }
        }
    }

    protected Object getMonitor() {
        return monitor;
    }

    protected List<Bot> getBots() {
        return bots;
    }
}
