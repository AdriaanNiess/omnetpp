#!/usr/bin/perl
#
# part of opp_neddoc -- renders NED comments into HTML, does syntax highlight,
# and exports images
#

$verbose = 0;

# parse tags.xml
$tagfile = "html/tags.xml";
print "reading $tagfile...\n" if ($verbose);
open(INFILE, $tagfile) || die "cannot open $tagfile";
read(INFILE, $tags, 1000000) || die "cannot read $tagfile";
$components = ();
while ($tags =~ s|(\<tag .*?/\>)||s)
{
      $tag = $1;
      if ($tag =~ m|name="(.*?)"|)     {$name = $1; push(@components,$name);} else {next;}
      if ($tag =~ m|type="(.*?)"|)     {$type{$name} = $1;}
      if ($tag =~ m|htmlfile="(.*?)"|) {$htmlfile{$name} = $1;}
      if ($tag =~ m|nedfile="(.*?)"|)  {$nedfile{$name} = $1;}
      if ($tag =~ m|comment="(.*?)"|)  {$comment{$name} = $1;}
      #print "DBG: $name, $type{$name}, $htmlfile{$name}, $nedfile{$name}, $comment{$name}\n";
}
#print join(' ', @components);


foreach $fnamepatt (@ARGV)
{
    foreach $fname (glob($fnamepatt))
    {
        #
        # read file
        #
        print "processing $fname...\n" if ($verbose);
        open(INFILE, $fname) || die "cannot open $fname";
        read(INFILE, $html, 1000000) || die "cannot read $fname";

        #
        # process comments
        #
        while ($html =~ s|\<pre class="(comment)">(.*?)\</pre\>|\@bingo\@|s ||
               $html =~ s|\<span class="(comment)">(.*?)\</span\>|\@bingo\@|s ||
               $html =~ s|\<span class="(briefcomment)">(.*?)\</span\>|\@bingo\@|s)
        {
              # process comment
              $class = $1;
              $comment = $2;

              # add sentries to facilitate processing
              $comment = "\n\n".$comment."\n\n";

              # remove '//-' lines (those are comments to be ignored by documentation generation)
              $comment =~ s|^ *//-.*||gm;

              # remove '//' from beginning of lines
              $comment =~ s|^ *// ?||gm;

              # extract existing <pre> sections to prevent tampering inside them
              $comment =~ s|&lt;pre&gt;(.*?)&lt;/pre&gt;|$pre{++$ctr}=$1;"<pre$ctr>"|gse;

              # insert blank line (for later processing) in front of lines beginning with '-'
              $comment =~ s|\n( *-)|\n\n\1|gs;

              # if briefcomment, keep only the 1st paragraph
              if ($class eq "briefcomment") {
                 $comment =~ s|(.*?[^\s].*?)\n\n.*|\1\n\n|gs;
              }

              # wrap paragraphs not beginning with '-' into <p></p>.
              # (note: (?=...) and (?<=...) constructs are lookahead and lookbehind assertions,
              # see e.g. http://tlc.perlarchive.com/articles/perl/pm0001_perlretut.shtml).
              $comment =~ s|(?<=\n\n)\s*([^- \t].*?)(?=\n\n)|<p>\1</p>|gs;

              # wrap paragraphs beginning with '-' into <li></li> and <ul></ul>
              $comment =~ s|(?<=\n\n)\s*-\s+(.*?)(?=\n\n)|  <ul><li>\1</li></ul>|gs;
              $comment =~ s|\</li\>\</ul\>\s*\<ul\>\<li\>|</li>\n\n  <li>|gs;

              # wrap paragraphs beginning with '-#' into <li></li> and <ol></ol>
              $comment =~ s|(?<=\n\n)\s*-#\s+(.*?)(?=\n\n)|  <ol><li>\1</li></ol>|gs;
              $comment =~ s|\</li\>\</ol\>\s*\<ol\>\<li\>|</li>\n\n  <li>|gs;

              # now we can put back <pre> regions
              $comment =~ s|\<pre(\d+)\>|'<pre>'.$pre{$1}.'</pre>'|gse;

              # now we can trim excess blank lines
              $comment =~ s|^\n+||;
              $comment =~ s|\n+$|\n|;

              # restore " from &quot; (important for attrs of html tags, see below)
              $comment =~ s|&quot;|"|gsi;

              # extract <nohtml> sections to prevent substituting inside them
              $comment =~ s|&lt;nohtml&gt;(.*?)&lt;/nohtml&gt;|$nohtml{++$ctr}=$1;"<nohtml$ctr>"|gse;

              # decode certain HTML tags: <i>,<b>,<br>,...
              $tags="a|b|body|br|center|caption|code|dd|dfn|dl|dt|em|font|form|hr|h1|h2|h3|i|input|img|li|meta|multicol|ol|p|small|span|strong|sub|sup|table|td|th|tr|tt|kbd|ul|var";
              $comment =~ s!&lt;(($tags)( [^\n]*?)?)&gt;!<\1>!gsi;
              $comment =~ s!&lt;(/($tags))&gt;!<\1>!gsi;

              # put back <nohtml> sections
              $comment =~ s|\<nohtml(\d+)\>|$nohtml{$1}|gse;

              # put hyperlinks on module names, etc.
              $names = join('|',@components);
              $comment =~ s!\b($names)\b!'<a href="'.$htmlfile{$1}.'">'.$1.'</a>'!gse;

              # put comment back
              $html =~ s/\@bingo\@/$comment/s;
        }

        #
        # syntax-highlight source
        #
        while ($html =~ s|\<pre class="src">(.*?)\</pre\>|\@bingo\@|s)
        {
              # process comment
              $src = $1;

              # trim excess blank lines
              $src =~ s|^\n+||;
              $src =~ s|\n+$|\n|;

              # extract strings into %strings to prevent tampering inside them
              $src =~ s|(&quot;[^\n]*?&quot;)|$strings{++$ctr}=$1;"<string$ctr>"|gse;

              # extract comments into %comments to prevent tampering inside them
              $src =~ s|(//.*)$|$comments{++$ctr}=$1;"<comment$ctr>"|gme;

              # syntax-highlight keywords (must be 1st, otherwise "class=" will get messed up!)
              $keywords = 'import|network|module|simple|channel|for|do|true|false|ref|ancestor|'.
                          'input|const|sizeof|endsimple|endmodule|endchannel|endnetwork|endfor|'.
                          'parameters|gates|gatesizes|submodules|connections|display|on|'.
                          'like|machines|to|if|index|nocheck|numeric|string|bool|anytype|'.
                          'cppinclude|struct|cobject|noncobject|message|class|enum|extends|fields|'.
                          'properties|abstract|char|short|int|long|double|delay|error|datarate';
              $src =~ s!\b($keywords)\b!\<span class="src-keyword"\>\1\</span\>!gs;

              # other special cases...
              $src =~ s!\b(in:|out:)!\<span class="src-keyword"\>\1\</span\>!gs;
              $src =~ s!(--&gt;|&lt;--|\.\.|\.\.\.)!\<span class="src-keyword"\>\1\</span\>!gs;

              # syntax-highlight numbers
              # (note: (?=...) and (?<=...) constructs are lookahead and lookbehind assertions,
              # see e.g. http://tlc.perlarchive.com/articles/perl/pm0001_perlretut.shtml).
              $src =~ s!(?<=[^0-9a-zA-Z_])(-?((\d+(\.\d+)?)|(\.\d+))([eE][+-]?\d+)?)!\<span class="src-number"\>\1\</span\>!gs;

              # put back strings, syntax-highlighted
              $src =~ s|\<string(\d+)\>|'<span class="src-string">'.$strings{$1}.'</span>'|gse;

              # put back comments, syntax-highlighted
              $src =~ s|\<comment(\d+)\>|'<span class="src-comment">'.$comments{$1}.'</span>'|gse;

              # put back strings in comments
              $src =~ s|\<string(\d+)\>|$strings{$1}|gse;

              # restore " from &quot; (important for attrs of html tags, see below)
              $src =~ s|&quot;|"|gsi;

              # kill embedded spans....
              #$src =~ s|\<span.*?</?span|span|g;

              # put comment back
              $html =~ s|\@bingo\@|\<pre\>$src\</pre\>|s;
        }

        #
        # save file
        #
        open(FILE,">$fname");
        print FILE $html;
        close FILE;
    }
}

# export_file('html/pccard.gif',
# 'R0lGODlhOAAfALMAABAUGRYtT0xWZH2CipShsLW2usXCv9DT1ube2Orq6vHw8/v18/P3//z5
# ...
# XCSFlkYnzH6WOoYcFhf/oXzVe4qeajQCGLDcPAqNSBu7V2F9wHwLEQAAOw==');

sub export_file ()
{
    my $fname = shift;
    my $base64str = shift;
    open FILE, ">$fname";
    binmode FILE;
    print FILE decode_base64($base64str);
    close FILE;
}

sub decode_base64 ($)
{
    # Code taken from MIME-Base64-2.20 which contains the following copyright:
    # "This library is free software; you can redistribute it and/or
    # modify it under the same terms as Perl itself."

    local($^W) = 0; # unpack("u",...) gives bogus warning in 5.00[123]
    my $str = shift;
    $str =~ tr|A-Za-z0-9+=/||cd;            # remove non-base64 chars
    if (length($str) % 4) {die 'length of base64 data not a multiple of 4';}
    $str =~ s/=+$//;                        # remove padding
    $str =~ tr|A-Za-z0-9+/| -_|;            # convert to uuencoded format
    return "" unless length $str;
    return unpack("u", join('', map( chr(32 + length($_)*3/4) . $_,
    			$str =~ /(.{1,60})/gs) ) );
}

