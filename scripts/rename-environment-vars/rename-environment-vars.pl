#!/usr/bin/perl -w
use strict;
my $inEnvironmentSection=0;
foreach my $line ( <STDIN> ) {
    chomp( $line );
	#uppercase environment variable name
	if ($inEnvironmentSection){
        if ($line =~ /^[\s#]+\w+:\s*$/) {
                $inEnvironmentSection=0;
                print $line . "\n";
        } else {
            if ($line =~ /^[\s#]+\- ".+=.+"\s*(#.*)?$/) {
                my $out = $line;
                $out =~ s/(\w+)(?=.*=.+$)/uc($1)/ge;
                $out =~ s/\.(?=.*=.+$)/_/g;
                print $out . "\n";
            } else {
                print $line . "\n";
            }
        }
    } else {
        if ($line =~ /^[\s#]+environment:\s*$/){
            $inEnvironmentSection=1;
        }
        print $line . "\n";
    }


}
