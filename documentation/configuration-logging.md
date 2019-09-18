# Logging

Logging in our project is done with **Simple Logging Facade for Java (SLF4J)** as facade and Logback as backend logging framework.

For configuring it, have a look at the `logback*.xml` files in the `[webofneeds/conf](webofneeds/conf)` folder.
The applications have to be told where to look for this file via a system property that depends on the kind of application.

- web apps: `-Dlogback.configurationFile=[path-to-your]/logback.xml`
- spring boot (e.g. bots): `-Dlogging.config=[path-to-your]/logback.xml`

# Log Levels

The following logging levels are used:

- **ERROR**: if the application fails to start up, or doesnt't work at all;
- **WARN**: situations where methods can't execute as they are to be expected (methods called in states where they are not allowed, parametes pointing to not existing atoms, etc.) but the application is still in a operative state; calls, data structures, etc., which go not conform with guidelines, good practices or hard constraints.
- **INFO**: only in infrequent calls; one time initializations; atom status changes (protocol calls);
- **DEBUG**: everything else.

We do not use TRACE (see http://www.slf4j.org/faq.html#trace).

# Usage for developers

- We make use of the {}-placeholder feature of slf4j (see http://www.slf4j.org/faq.html#logging_performance).
- If you need expensive calculations to get the logging string make use of the `logger.isDebugEnabled()`, etc. function.

# Exceptions

Each exception has to be logged with an according message besides the stack trace. If there is no need for a special message use
`logger.warn("caught <NameOfException>:", e)`
