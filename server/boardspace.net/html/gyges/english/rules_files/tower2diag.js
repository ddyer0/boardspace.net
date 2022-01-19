// towers2diag: Written by Joao Neto based on code by Hans Bodlaender (c) 2001, 2002
// 
// Non-commercial use is permitted: `no fee - no guarantee' 
// For commercial use, ask Hans at hans@chessvariants.com and Joao at jpn@di.fc.ul.pt

function draw_empty(path) {

document.write("<td width=\"44\" height=\"1\"><center>");
document.write("<table border=\"0\" width=\"14\" cellspacing=\"0\" cellpadding=\"0\" height=\"12\">");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"half.gif\" width=\"14\" height=\"4\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"half.gif\" width=\"14\" height=\"4\"></td></tr>");
document.write("</table></center></td>");

}

//*************************************************

function draw_empty_cell(path) {

document.write("<td width=\"44\" height=\"1\" background=\""+document.body.background+"\"><center>");
document.write("<table border=\"0\" width=\"14\" cellspacing=\"0\" cellpadding=\"0\" height=\"12\">");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"half.gif\" width=\"14\" height=\"4\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"half.gif\" width=\"14\" height=\"4\"></td></tr>");
document.write("</table></center></td>");

}

//*************************************************

function draw_tower(path,a,b,c,d,e,f) {

document.write("<td width=\"44\" height=\"1\"><center>");
document.write("<table border=\"0\" width=\"14\" cellspacing=\"0\" cellpadding=\"0\" height=\"12\">");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"half.gif\" width=\"14\" height=\"4\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+ a +".gif\" width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+ b +".gif\" width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+ c +".gif\" width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+ d +".gif\" width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+ e +".gif\" width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+ f +".gif\" width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"half.gif\" width=\"14\" height=\"4\"></td></tr>");
document.write("</table></center></td>");

}

//*************************************************

function draw_symbol(path,s) {

document.write("<td width=\"44\" height=\"1\"><center>");
document.write("<table border=\"0\" width=\"14\" cellspacing=\"0\" cellpadding=\"0\" height=\"12\">");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"half.gif\" width=\"14\" height=\"4\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+ s +".gif\" width=\"14\" height=\"24\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"e.gif\"    width=\"14\" height=\"6\"></td></tr>");
document.write("<tr><td width=\"100%\"><img border=\"0\" src=\""+path+"half.gif\" width=\"14\" height=\"4\"></td></tr>");
document.write("</table></center></td>");

}

//*************************************************

function isUpper( c ) {
   return (("A"<=c) && (c<="Z"))
}

//*************************************************

function tower2diag(back,boardstr) {
 //  document.write(boardstr);
   tower2diagp(back,boardstr,"");
}

function towergv2diag(back,boardstr) {  //jpn note: to use in the WAG website
   tower2diagp(back, boardstr,"tower2diag/");
}

//*************************************************

function tower2diagp(background, boardstr, gifpath) {

    color = "#D5BD83";

    // parse boardstr
    strlength = boardstr.length;
    strpos    = 0;
    curchar   = boardstr.charAt(strpos);

    // first, an integer that gives the number of rows
    nbrows = 0;
    while (strpos < strlength && ("0" <= curchar) && (curchar <= "9"))
    {
         nbrows = 10*nbrows + parseInt(curchar);
	 strpos++;
	 curchar = boardstr.charAt(strpos);
    }

    // skip the comma
    if (curchar == ",")	curchar = boardstr.charAt(++strpos);

    // now an integer follows that gives the number of columns
    nbcolumns = 0;
    while (strpos < strlength && ("0" <= curchar) && (curchar <= "9"))
    {
         nbcolumns = 10*nbcolumns + parseInt(curchar);
	 curchar = boardstr.charAt(++strpos);
    }

    // skip the comma
    if (curchar == ",")	curchar = boardstr.charAt(++strpos);

    //****************************************************
    // and now we get to the part that describes the board 
    // so, start table:

    width_table = 44*nbcolumns;

    document.write("<table border=\"1\" width=\""+width_table+"\" bordercolor=\"#000000\" bgcolor=\"#D5BD83\" background=\""+background+"\" height=\"92\" cellspacing=\"0\" cellpadding=\"0\" >")

    row = 1; column = 1;
    while (strpos < strlength) {

	curchar = boardstr.charAt(strpos);
        check = 0; // check tells: did we match this char?

	// options: what are we reading in the string
	// first case: a stack of black/white stones

        if (("0" <= curchar) && (curchar <= "9") ) {

            switch (parseInt(curchar)) {
               case 1 : draw_tower(gifpath,"e","e","e","e","e","x"); break;
               case 2 : draw_tower(gifpath,"e","e","e","e","x","x"); break;
               case 3 : draw_tower(gifpath,"e","e","e","x","x","x"); break;
               case 4 : draw_tower(gifpath,"e","e","x","x","x","x"); break;
               case 5 : draw_tower(gifpath,"e","x","x","x","x","x"); break;
               case 6 : draw_tower(gifpath,"e","e","e","e","e","w"); break;
               case 7 : draw_tower(gifpath,"e","e","e","e","w","w"); break;
               case 8 : draw_tower(gifpath,"e","e","e","w","w","w"); break;
               case 9 : draw_tower(gifpath,"e","e","w","w","w","w"); break;
               case 0 : draw_tower(gifpath,"e","w","w","w","w","w"); break;
            };

 	    column++;
            strpos++; 
 	    check = 1;
        }
	
        // second case: empty cell

        if ("." == curchar) {
          draw_empty(gifpath);
          column++;
          strpos++; 
          check = 1;
        }
	
       // third case: end of row marker: '/'

       if (curchar == "/") {
          document.write("</tr><tr>");
          row = row + 1; 
          column = 1;
          strpos++; 
          check = 1;
       }

       // fourth case: arbitrary stack (it must have 6 chars)

       if (curchar == "[") {

  	    a1 = boardstr.charAt(++strpos);  if (isUpper(a1)) a1 = "k" + a1.toLowerCase();
  	    a2 = boardstr.charAt(++strpos);  if (isUpper(a2)) a2 = "k" + a2.toLowerCase();
  	    a3 = boardstr.charAt(++strpos);  if (isUpper(a3)) a3 = "k" + a3.toLowerCase();
  	    a4 = boardstr.charAt(++strpos);  if (isUpper(a4)) a4 = "k" + a4.toLowerCase();
  	    a5 = boardstr.charAt(++strpos);  if (isUpper(a5)) a5 = "k" + a5.toLowerCase();
    	    a6 = boardstr.charAt(++strpos);  if (isUpper(a6)) a6 = "k" + a6.toLowerCase();
            strpos++;  // skip the ]

            draw_tower(gifpath,a1,a2,a3,a4,a5,a6);

            row = row + 1; 
            strpos++; 
            check = 1;

       }

       //fifth case: other letters

       if (("a" <= curchar) && (curchar <= "z") ) {

          switch (curchar.charAt(0)) {
             case 'a' : draw_symbol(gifpath,"1"); break;
             case 'b' : draw_symbol(gifpath,"2"); break;
             case 'c' : draw_symbol(gifpath,"3"); break;
             case 'd' : draw_symbol(gifpath,"4"); break;
             case 'e' : draw_symbol(gifpath,"5"); break;
             case 'f' : draw_symbol(gifpath,"6"); break;
             case 'g' : draw_symbol(gifpath,"7"); break;
             case 'h' : draw_symbol(gifpath,"8"); break;
             case 'i' : draw_symbol(gifpath,"9"); break;
             case 'z' : draw_empty_cell(gifpath); break;
          };

            row = row + 1; 
            strpos++; 
            check = 1;
       }

       //sixth case: other background colors - not functioning...

       if (curchar == "{") {

          switch (curchar.charAt(0)) {
             case 'r' : color = "#FF0000"; break;
             case 'g' : color = "#00FF00"; break;
             case 'b' : color = "#0000FF"; break;
             case 'z' : color = "#D5BD83"; break;  // standard color
          };

          strpos++;  
          strpos++;   // skip the }
          check = 1;

       }

       // if character hasn't been matched, we just skip it this avoids infinite loops
       if (check == 0) strpos++;

   }  // end while()

 document.write("</tr></table>");
}

