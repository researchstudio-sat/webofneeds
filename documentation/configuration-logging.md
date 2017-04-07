Logging in our project is done with **Simple Logging Facade for Java (SLF4J)** as facade and Apache log4j as backend logging framework.

The following logging levels are used:
* **ERROR**: if the application fails to start up, or doesnt't work at all;
* **WARN**: situations where methods can't execute as they are to be expected (methods called in states where they are not allowed, parametes pointing to not existing needs, etc.) but the application is still in a operative state; calls, data structures, etc., which go not conform with guidelines, good practices or hard constraints.
* **INFO**: only in infrequent calls; one time initializations; need status changes (protocol calls);
* **DEBUG**: everything else.

We do not use TRACE (see http://www.slf4j.org/faq.html#trace).

### Usage
* We make use of the {}-placeholder feature of slf4j (see http://www.slf4j.org/faq.html#logging_performance).
* If you need expensive calculations to get the logging string make use of the `logger.isDebugEnabled()`, etc. function.

### Exceptions
Each exception has to be logged with an according message besides the stack trace. If there is no need for a special message use 
`logger.warn("caught <NameOfException>:", e)`

