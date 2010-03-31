#!/usr/bin/perl

$ts = qr/\[\d\d:\d\d:\d\d\]/;

$a = 'Alpha';
$b = 'Beta';
$opt_regex = qr/^-(.+)/;
if (exists($ARGV[0]))
{
	$c = $ARGV[0];
	if ($c eq '-')
	{
		shift @ARGV;
		($a,$b) = ($b,$a);
	}
	elsif ($c =~ /$opt_regex/)
	{
		shift @ARGV;
		$a = $1;
		
		if (exists($ARGV[0]))
		{
			$c = $ARGV[0];
			if ($c =~ /$opt_regex/)
			{
				shift @ARGV;
				$b = $1;
			}
		}
	}
}

while (<>)
{
	if ($_ =~ /^$ts /)
	{
		if (($_ =~ s/^$ts (?:$b|\{\{from $b\}\})/Stranger/) ||
			($_ =~ s/^$ts (?:$a|<<$a>>)/You/))
		{
			print;
		}
	}
	else
	{
		print;
	}
}
