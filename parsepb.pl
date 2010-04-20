#!/usr/bin/perl

# alternative paths to look for files when relative to enclosing group
@path=("../JRclient");

#$pf="/Network/Servers/phaeton.math.uni-augsburg.de/NetUsers/urbaneks/develop/org/rosuda/Mondrian/Mondrian.pbproj";
$pf=shift;

if ($pf eq '') {
    print "Usage: parsepb <*.pbproj> [<options>]\n\nReturns all files used in the main build.\n Options: -ext:<ext> return the files with the corresponding extension only\n";
    exit 1;
}

$op=shift;
while($op ne '') {
    if ($op=~/-ext:(.*)/) {
        push @filter, $1;
    }
    $op=shift;
}

$pbx="$pf/project.pbxproj";

open IN, $pbx;
$stage=0;
while (<IN>) {
    $stage=1 if (/objects = {/);

if (/([0-9A-Z]{24}) = {/) {
    $ref=$1;
    $file='';
    $infile=1;
}
if (/};/ && $infile==1) {
    if ($file ne '') {
        $fref{$ref}=$file;
    }
    if ($fileref ne '') {
        $iref{$ref}=$fileref;
    }
    $infile=0;
}
if ($infile==1) {
    if (/path = (.*);/) {
        $file=$1;
    }
    if (/fileRef = (.*);/) {
        $fileref=$1;
    }
}
if ($inbuild==1 && /([0-9A-Z]{24}),/) {
    push @build, $1;
} else {
    if ($inbuild==1) {
        $inbuild=2;
    }
}
if (/files = \(/ && $inbuild==0) { $inbuild=1; }
}
close IN;

foreach(@build) {
    $t=$fref{$_};
    $t=$fref{$iref{$_}} if ($t eq '' && $iref{$_} ne '');
    $matches=1;
    if ($filter[0] ne '') {
        $matches=0;
        foreach(@filter) {
            $matches++ if ($t=~/\.$_$/);
        }
    }
    if($matches>0) {
	$ok=0;
	if (-e $t) {
	    print "$t ";
	    $ok=1;
	} else {
	    foreach(@path) { if (-e "$_/$t") { print "$_/$t "; $ok=1; last; } }
	}
	if ($ok==0) {
	    print STDERR "(parsepb.warning: $t not found, adding anyway)";
	    print "$t ";
	}
    }
}

#foreach(sort(keys(%fref))) { print "$_\t$fref{$_}\n"; };
print "\n";

