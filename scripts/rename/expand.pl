#!/usr/bin/perl -w
foreach my $line ( <STDIN> ) {
    chomp( $line );
	print $line . "\n";
	if ($line =~ /^#/){
		#don't expand comments
		next;
	}
	#uppercase first letter
	$out = $line;
	$out =~ s/^(\p{IsLower})/uc($1)/ge;
	print $out . "\n";
	#lowercase first letter
	$out = $line;
	$out =~ s/^(\p{IsUpper})/lc($1)/ge;
	print $out . "\n";
	#toupper
	$out = $line;
	$out =~ s/(\p{IsLower})/uc($1)/ge;
	print $out . "\n";
	#tolower
	$out = $line;
	$out =~ s/(\p{IsUpper})/lc($1)/ge;
	print $out . "\n";
	## camel case to uppercase underscored
	$out = $line;
	$out =~ s/(\p{IsLower})(\p{IsUpper})/$1_$2/g;
	$out =~ s/(\p{IsLower})/uc($1)/ge;
	print $out . "\n";
	## camel case to lowercase underscored
	$out = $line;
	$out =~ s/(\p{IsLower})(\p{IsUpper})/$1_$2/g;
	$out =~ s/(\p{IsUpper})/lc($1)/ge;
	print $out . "\n";
	## camel case to uppercase hyphenated
	$out = $line;
	$out =~ s/(\p{IsLower})(\p{IsUpper})/$1-$2/g;
	$out =~ s/(\p{IsLower})/uc($1)/ge;
	print $out . "\n";
	## camel case to lowercase hyphenated
	$out = $line;
	$out =~ s/(\p{IsLower})(\p{IsUpper})/$1-$2/g;
	$out =~ s/(\p{IsUpper})/lc($1)/ge;
	print $out . "\n";
}
