import java.io.*;

public class HackAssembler extends Code {
    private SymbolTable table;
    private Parser parser;
    static int nextVarAddress = 16; //keeps track of next location for A variable

    public HackAssembler(File f) throws IOException {
        this.table = new SymbolTable();
        this.parser = new Parser(f);
        parser.mark((int) f.length());
    }
    private void handleAInstruction(BufferedWriter writer, int lineNum) throws IOException {
        if (table.contains(parser.symbol())) {
            toBinaryAndWrite(writer, table.getAddress(parser.symbol()));
        } else {
            try {
                //if the symbol is an address, we convert to binary and write
                int address = Integer.parseInt(parser.symbol());
                toBinaryAndWrite(writer, address);
            } catch (NumberFormatException e) {
                //if it's not a number, we add to table, convert to binary and write
                //the address is appended after all previous A variable
                table.addEntry(parser.symbol(), nextVarAddress);
                nextVarAddress++;
                toBinaryAndWrite(writer, table.getAddress(parser.symbol()));
            }
        }

    }
    private void handleCInstruction(BufferedWriter writer) throws IOException {
        String binAddress = "111" + Code.comp(parser.getComp()) + Code.dest(parser.getDest()) + Code.jump(parser.getJump());
        writer.write(binAddress);
        writer.newLine();
    }

    /**
     * Performs the first iteration on the file.
     * Finds only L_INSTRUCTION, and adds their symbol and line number to the table
     *
     * @throws IOException
     */
    public void firstPass() throws IOException {
        int lineNum = 0;
        while (this.parser.hasMoreLines()) {
            parser.advance();
            if (parser.instructionType()== InstructType.L_INSTRUCTION) {
                table.addEntry(parser.symbol(), lineNum);
            }
            if (parser.instructionType() == InstructType.A_INSTRUCTION || parser.instructionType() == InstructType.C_INSTRUCTION) {
                lineNum++;
            }
        }
        parser.resetParser();
    }

    private void toBinaryAndWrite(BufferedWriter writer, int address) throws IOException {
        //convert the address of the symbol to a binary string
        //write it to file
        String binAddress = Integer.toBinaryString(address);
        while (binAddress.length() < 16) {
            binAddress = '0' + binAddress;
        }
        writer.write(binAddress);
        writer.newLine();
    }

    /**
     * Performs the second iteration on the file.
     * Iterates line by line, converting the assembly commands into binary strings
     * Writes said binary strings into a new file - Prog.hack.
     *
     * @throws IOException
     */
    public void secondPass(String path) throws IOException {
        //create a new file with the same name as the original file,
        // at the same location, but with a .hack extension
        BufferedWriter writer = new BufferedWriter(new FileWriter(path + ".hack"));
        nextVarAddress = 16; //make sure nextVarAddress is reset for each file

        int lineNum = 0;
        while (this.parser.hasMoreLines()) {
            parser.advance();
            if (parser.instructionType() == InstructType.C_INSTRUCTION || parser.instructionType() == InstructType.A_INSTRUCTION) {
                lineNum++;
            }
            //Handling A Instructions
            if (parser.instructionType() == InstructType.A_INSTRUCTION) {
                handleAInstruction(writer, lineNum);
            }
            //Handling C instructions
            if (parser.instructionType() == InstructType.C_INSTRUCTION) {
                handleCInstruction(writer);
            }
        }
        writer.close();
    }
}
