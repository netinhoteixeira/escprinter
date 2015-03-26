This very simple library allows printing to Epson ESC/P and ESC/P2 matrix printers from Java programs.

Currently tested with the Epson LX-300+II 9 pin impact printer. But 24 pin support is included.

Usage:
Printer should be shared on the network. E.g. \\computername\printershare

You use this network path when constructing ESCPrinter objects.

//create ESCPrinter on network location \\computer\sharename
//false indicates 9pin printer (use true for 24pin)

ESCPrinter escp = new ESCPrinter("\\\\computername\\printershare", false);



if (escp.initialize()) {
> escp.setCharacterSet(ESCPrinter.USA);

> escp.select15CPI(); //15 characters per inch printing

> escp.advanceVertical(5); //go down 5cm

> escp.setAbsoluteHorizontalPosition(5); //5cm to the right

> escp.bold(true);

> escp.print("Let's print some matrix text ;)");

> escp.bold(false);

> escp.advanceVertical(1);

> escp.setAbsoluteHorizontalPosition(5); //back to 5cm

> escp.print("Very simple and easy!");

> escp.formFeed(); //eject paper

> escp.close(); //close stream
}
else
> System.out.println("Couldn't open stream to printer");