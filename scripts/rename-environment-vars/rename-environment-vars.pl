#!/usr/bin/perl -w
use strict;
my $inEnvironmentSection=0;
foreach my $line ( <STDIN> ) {
    chomp( $line );
	#uppercase environment variable name
	if ($inEnvironmentSection){
        if ($line =~ /^#?\s+\-\s+"((\w+)\.?)+=.+\"$/) {
            my $out = $line;
            $out =~ s/(\w+)(?=.*=.+$)/uc($1)/ge;
            print $out . "\n";
        } else {
            print $line . "\n";
        }
        if ($line =~ /^#?\s+\w+:\s*$/) {
                $inEnvironmentSection=0;
        }
    } else {
        if ($line =~ /^#?\s+environment:\s*$/){
            $inEnvironmentSection=1;
        }
        print $line . "\n";
    }


}
