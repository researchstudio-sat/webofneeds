# Logging

Logging in our project is done with **Simple Logging Facade for Java (SLF4J)** as facade and Logback as backend logging framework.

For configuring it, have a look at the `logback*.xml` files in the `[webofneeds/conf](webofneeds/conf)` folder.
The applications have to be told where to look for this file via a system property that depends on the kind of application.

- web apps: `-Dlogback.configurationFile=[path-to-your]/logback.xml`
- spring boot (e.g. bots): `-Dlogging.config=[path-to-your]/logback.xml`

# Log Levels

We use ERROR, WARN, INFO and DEBUG. You can find documentation on these [here](https://logging.apache.org/log4j/2.x/manual/architecture.html#Log_Levels) and a basic tutorial [here](https://www.tutorialspoint.com/log4j/log4j_logging_levels.htm).

# Usage for developers

- We make use of the {}-placeholder feature of slf4j (see http://www.slf4j.org/faq.html#logging_performance).
- If you need expensive calculations to get the logging string make use of the `logger.isDebugEnabled()`, etc. function.

# Exceptions

Each exception has to be logged with an according message besides the stack trace. If there is no need for a special message use
`logger.warn("caught <NameOfException>:", e)`
