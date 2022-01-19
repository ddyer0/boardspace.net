/* net.sf.jazzlib.ZipConstants
   Copyright (C) 2001 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package jzlib;
/**
 * there's a problem with java standard implementation of zip decoding, when the file names
 * contain UTF8 names.  This bug has been known to sun for at least 6 years, but they've never
 * seend fit to fix it, despite UTF8 names becoming more and more common.
 * 
 * At some point in the evolution of this project, file names for foreign
 * guests started having diacritical marks, and this triggers the bug.  The effect of the bug
 * is that readng a zip file gets an "illegal argument exception", which caused tantrix and
 * boardspace to abort reading the zip file.
 * 
 * this solution is a little better than a hack, based on solution #1 at 
 * http://www.peterbuettner.de/develop/javasnippets/zipOnlyAscii/index.html
 * which combines part of a pure-java zip reader called jazzlib with the guts
 * of the standard reader, which is fast and proven reliable (except for the bug
 * in hand of course)
 * 
 * @author ddyer
 * 8/2010
 */
interface ZipConstants
{
  /* The local file header */
  int LOCHDR = 30;
  int LOCSIG = 'P'|('K'<<8)|(3<<16)|(4<<24);

  int LOCVER =  4;
  int LOCFLG =  6;
  int LOCHOW =  8;
  int LOCTIM = 10;
  int LOCCRC = 14;
  int LOCSIZ = 18;
  int LOCLEN = 22;
  int LOCNAM = 26;
  int LOCEXT = 28;

  /* The Data descriptor */
  int EXTSIG = 'P'|('K'<<8)|(7<<16)|(8<<24);
  int EXTHDR = 16;

  int EXTCRC =  4;
  int EXTSIZ =  8;
  int EXTLEN = 12;

  /* The central directory file header */
  int CENSIG = 'P'|('K'<<8)|(1<<16)|(2<<24);
  int CENHDR = 46;

  int CENVEM =  4;
  int CENVER =  6;
  int CENFLG =  8;
  int CENHOW = 10;
  int CENTIM = 12;
  int CENCRC = 16;
  int CENSIZ = 20;
  int CENLEN = 24;
  int CENNAM = 28;
  int CENEXT = 30;
  int CENCOM = 32;
  int CENDSK = 34;
  int CENATT = 36;
  int CENATX = 38;
  int CENOFF = 42;

  /* The entries in the end of central directory */
  int ENDSIG = 'P'|('K'<<8)|(5<<16)|(6<<24);
  int ENDHDR = 22;

  /* The following two fields are missing in SUN JDK */
  int ENDNRD =  4;
  int ENDDCD =  6;
  int ENDSUB =  8;
  int ENDTOT = 10;
  int ENDSIZ = 12;
  int ENDOFF = 16;
  int ENDCOM = 20;
}

