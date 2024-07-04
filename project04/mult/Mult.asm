// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// Assumes that R0 >= 0, R1 >= 0, and R0 * R1 < 32768.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)

//RAM[2] <- 0, for initialization
@2
M=0
//Setting var x=R0
@0
D=M
@x
M=D

(LOOP)
	@1
	//if (i=0) go to CONT
	D=M
	@CONT
	D;JEQ

	//R2=R2+x
	@x
	D=M
	@2
	M=M+D

	//RAM[1]=RAM[1]-1
	@1
	M=M-1
	//Go back to loop
	@LOOP
	0;JMP
(CONT)
(END)
	@END
	0;JMP
