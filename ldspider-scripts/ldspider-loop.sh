#!/bin/sh
SLEEPTIME=30
while(true) 
	do 
		./ldspider-run.sh 
		echo finished ldspider run, sleeping for $SLEEPTIME seconds
		sleep $SLEEPTIME
		echo waking up and re-running ldspider 
	done
