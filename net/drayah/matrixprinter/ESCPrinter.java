/*
 
Copyright (c) 2006-2007, Giovanni Martina
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that 
the following conditions are met:

- Redistributions of source code must retain the above copyright notice, this list of conditions and the 
following disclaimer.

- Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
following disclaimer in the documentation and/or other materials provided with the distribution.

-Neither the name of Drayah, Giovanni Martina nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED 
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY 
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH 
DAMAGE.
 * 
 * ESCPrinter.java
 *
 * Created on 10 de Setembro de 2006, 13:57
 *
 * @author Giovanni <gio@drayah.net>
 * Copyright © 2006 G.M. Martina
 *
 * Class that enables printing to ESC/P and ESC/P2 dot matrix printers (e.g. Epson LQ-570, Epson LX-300) by writing directly to a stream using standard I/O
 * Like this we have direct control over the printer and bypass Java2D printing which is considerably slower printing in graphics to a dotmatrix
 *
 */

package net.drayah.matrixprinter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class ESCPrinter {
    /** Creates a new instance of ESCPrinter */
    public ESCPrinter(String printer, boolean escp24pin) {
        //pre: printer nonnull String indicating network path to printer
        this.printer = printer;
        this.escp24pin = escp24pin;
    }
    
    public void close() {
        //post: closes the stream, used when printjob ended
        try {
            pstream.close();
            ostream.close();
        } 
        catch (IOException e) { e.printStackTrace(); }
    }
    
    public boolean initialize() {
        //post: returns true iff stream to network printer successfully opened, streams for writing to esc/p printer created
        streamOpenSuccess = false;
        
        try {
            //create stream objs
            ostream = new FileOutputStream(printer);
            pstream = new PrintStream(ostream);
            
            //reset default settings
            pstream.print(ESC);
            pstream.print(AT);
            
            //select 10-cpi character pitch
            select10CPI();
            
            //select draft quality printing
            selectDraftPrinting();
            
            //set character set
            setCharacterSet(BRAZIL);
            streamOpenSuccess = true;
        } 
        catch (FileNotFoundException e) { e.printStackTrace(); }
        
        return streamOpenSuccess;
    }
    
    public void select10CPI() { //10 characters per inch (condensed available)
        pstream.print(ESC);
        pstream.print(P);
    }
    
    public void select15CPI() { //15 characters per inch (condensend not available)
        pstream.print(ESC);
        pstream.print(g);
    }
    
    public void selectDraftPrinting() { //set draft quality printing
        pstream.print(ESC);
        pstream.print(x);
        pstream.print((char) 48);
    }
    
    public void selectLQPrinting() { //set letter quality printing
        pstream.print(ESC);
        pstream.print(x);
        pstream.print((char) 49);
    }
    
    public void setCharacterSet(char charset) {
        //assign character table
        pstream.print(ESC);
        pstream.print(PARENTHESIS_LEFT);
        pstream.print(t);
        pstream.print(ARGUMENT_3); //always 3
        pstream.print(ARGUMENT_0); //always 0
        pstream.print(ARGUMENT_1); //selectable character table 1
        pstream.print(charset); //registered character table (arg_25 is brascii)
        pstream.print(ARGUMENT_0); //always 0
        
        //select character table
        pstream.print(ESC);
        pstream.print(t);
        pstream.print(ARGUMENT_1); //selectable character table 1
    }
    
    public void lineFeed() {
        //post: performs new line
        pstream.print(CR); //according to epson esc/p ref. manual always send carriage return before line feed
        pstream.print(LINE_FEED);
    }
    
    public void formFeed() {
        //post: ejects single sheet
        pstream.print(CR); //according to epson esc/p ref. manual it is recommended to send carriage return before form feed
        pstream.print(FF);
    }
    
    public void bold(boolean bold) {
        pstream.print(ESC);
        if (bold)
            pstream.print(E);
        else
            pstream.print(F);
    }
    
    public void proportionalMode(boolean proportional) {
        pstream.print(ESC);
        pstream.print(p);
        if (proportional)
            pstream.print((char) 49);
        else
            pstream.print((char) 48);
    }
    
    public void advanceVertical(float centimeters) {
        //pre: centimeters >= 0 (cm)
        //post: advances vertical print position approx. y centimeters (not precise due to truncation)
        float inches = centimeters / CM_PER_INCH;
        int units = (int) (inches * (escp24pin ? MAX_ADVANCE_24PIN : MAX_ADVANCE_9PIN));
        
        while (units > 0) {
            char n;
            if (units > MAX_UNITS)
                n = (char) MAX_UNITS; //want to move more than range of parameter allows (0 - 255) so move max amount
            else
                n = (char) units; //want to move a distance which fits in range of parameter (0 - 255)
                        
            pstream.print(ESC);
            pstream.print(J);
            pstream.print(n);
            
            units -= MAX_UNITS;
        }
    }
    
    public void advanceHorizontal(float centimeters) {
        //pre: centimeters >= 0
        //post: advances horizontal print position approx. centimeters
        float inches = centimeters / CM_PER_INCH;
        int units_low = (int) (inches * 120) % 256;
        int units_high = (int) (inches * 120) / 256;
        
        pstream.print(ESC);       
        pstream.print(BACKSLASH);
        pstream.print((char) units_low);
        pstream.print((char) units_high);
    }
    
    public void setAbsoluteHorizontalPosition(float centimeters) {
        //pre: centimenters >= 0 (cm)
        //post: sets absolute horizontal print position to x centimeters from left margin
        float inches = centimeters / CM_PER_INCH;
        int units_low = (int) (inches * 60) % 256;
        int units_high = (int) (inches * 60) / 256;
        
        pstream.print(ESC);
        pstream.print($);
        pstream.print((char) units_low);
        pstream.print((char) units_high);
    }
    
    public void horizontalTab(int tabs) {
        //pre: tabs >= 0
        //post: performs horizontal tabs tabs number of times
        for (int i = 0; i < tabs; i++)
            pstream.print(TAB);
    }
    
    public void setMargins(int columnsLeft, int columnsRight) {
        //pre: columnsLeft > 0 && <= 255, columnsRight > 0 && <= 255
        //post: sets left margin to columnsLeft columns and right margin to columnsRight columns
        //left
        pstream.print(ESC);
        pstream.print(l);
        pstream.print((char) columnsLeft);
        
        //right
        pstream.print(ESC);
        pstream.print(Q);
        pstream.print((char) columnsRight);
    }
    
    public void print(String text) {
        pstream.print(text);
    }
    
    public boolean isInitialized() {
        //post: returns true iff printer was successfully initialized
        return streamOpenSuccess;
    }
    
    public String getShare() {
        //post: returns printer share name (Windows network)
        return printer;
    }
    
    public String toString() {
        //post: returns String representation of ESCPrinter e.g. <ESCPrinter[share=...]>
        StringBuilder strb = new StringBuilder();
        strb.append("<ESCPrinter[share=").append(printer).append(", 24pin=").append(escp24pin).append("]>");
        return strb.toString();
    }
    
    /* fields */
    private String printer;
    private boolean escp24pin; //boolean to indicate whether the printer is a 24 pin esc/p2 epson
    private FileOutputStream ostream;
    private PrintStream pstream;
    private boolean streamOpenSuccess;
    private static int MAX_ADVANCE_9PIN = 216; //for 24/48 pin esc/p2 printers this should be 180
    private static int MAX_ADVANCE_24PIN = 180;
    private static int MAX_UNITS = 127; //for vertical positioning range is between 0 - 255 (0 <= n <= 255) according to epson ref. but 255 gives weird errors at 1.5f, 127 as max (0 - 128) seems to be working
    private static final float CM_PER_INCH = 2.54f;
    
    /* decimal ascii values for epson ESC/P commands */
    private static final char ESC = 27; //escape
    private static final char AT = 64; //@
    private static final char LINE_FEED = 10; //line feed/new line
    private static final char PARENTHESIS_LEFT = 40;
    private static final char BACKSLASH = 92;
    private static final char CR = 13; //carriage return
    private static final char TAB = 9; //horizontal tab
    private static final char FF = 12; //form feed
    private static final char g = 103; //15cpi pitch
    private static final char p = 112; //used for choosing proportional mode or fixed-pitch
    private static final char t = 116; //used for character set assignment/selection
    private static final char l = 108; //used for setting left margin
    private static final char x = 120; //used for setting draft or letter quality (LQ) printing
    private static final char E = 69; //bold font on
    private static final char F = 70; //bold font off
    private static final char J = 74; //used for advancing paper vertically
    private static final char P = 80; //10cpi pitch
    private static final char Q = 81; //used for setting right margin
    private static final char $ = 36; //used for absolute horizontal positioning
    private static final char ARGUMENT_0 = 0;
    private static final char ARGUMENT_1 = 1;
    private static final char ARGUMENT_2 = 2;
    private static final char ARGUMENT_3 = 3;
    private static final char ARGUMENT_4 = 4;
    private static final char ARGUMENT_5 = 5;
    private static final char ARGUMENT_6 = 6;
    private static final char ARGUMENT_7 = 7;
    private static final char ARGUMENT_25 = 25;
    
    /* character sets */
    public static final char USA = ARGUMENT_1;
    public static final char BRAZIL = ARGUMENT_25;
}
