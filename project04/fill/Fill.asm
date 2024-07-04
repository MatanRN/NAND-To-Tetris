// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen
// by writing 'black' in every pixel;
// SCREEN=16384
// KBD=24576
// (row,col)=SCREEN+32*row+col/16
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen by writing
// 'white' in every pixel;
// the screen should remain fully clear as long as no key is pressed.

	//Checking condition
	(COND)
	@24576
	//if (i=0) go to WHITE
	D=M
	@WHITEINIT
	D;JEQ
	//else go to BLACK
	@BLACKINIT
	D;JGT


	(BLACKINIT)
	//initializing R0 with amount of times to loop to change entire screen
	@8192
	D=A
	@R0
	M=D
	
	//initializing R1 to track current location
	@R1
	D=0
	M=D
	@BLACKLOOP
	0;JMP

	(BLACKLOOP)
	//check if finished iterating
	@R0
	D=M
	D;JEQ

	//Decrease 1 blackened bits from loop counter
	@1
	D=A
	@R0
	M=M-D

	//Change the current pixel to black
	@R1
	D=M
	@SCREEN
	A=A+D
	M=-1

	//Add 1 to current location to counter
	@1
	D=A
	@R1
	M=M+D

	@BLACKLOOP
	0;JMP

	(WHITEINIT)
	//initializing R0 with amount of times to loop to change entire screen
	@8192
	D=A
	@R0
	M=D
	//initializing R1 to track current location
	@R1
	M=0
	@WHITELOOP
	0;JMP

	(WHITELOOP)
	//check if finished iterating
	@R0
	D=M
	D;JEQ

	//Decrease 16 whitened bits from loop counter
	@1
	D=A
	@R0
	M=M-D

	//Change the current pixel to white
	@R1
	D=M
	@SCREEN
	A=A+D
	M=0

	//Add 1 to current location to counter
	@1
	D=A
	@R1
	M=M+D

	@WHITELOOP
	0;JMP



