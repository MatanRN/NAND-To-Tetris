import java.io.*;


public class Parser extends BufferedReader {
    private InstructType currentType;
    private String currentSymbol;
    private String dest;
    private String comp;
    private String jump;

    public Parser(File f) throws FileNotFoundException {
        super(new FileReader(f));
        this.currentType = null;
        this.currentSymbol = null;
        this.dest = null;
        this.comp = null;
        this.jump = null;
    }

    /**
     * Helper function for setting the type of instruction.
     * Used every time an instruction is read.
     *
     * @param currentLine the line that was read
     */
    private void typeCheck(String currentLine) {
        if (currentLine.isEmpty() || currentLine.charAt(0) == ' ' || currentLine.charAt(0) == '/') {
            this.currentType = InstructType.NON_INSTRUCTION;
            return;
        }
        switch (currentLine.charAt(0)) {
            case '@':
                this.currentType = InstructType.A_INSTRUCTION;
                return;
            case '(':
                this.currentType = InstructType.L_INSTRUCTION;
                return;
            default:
                this.currentType = InstructType.C_INSTRUCTION;
                return;
        }
    }

    /**
     * Helper function for the entire parsing operation.
     *
     * @param currentLine the line that was read
     */
    private void paramParser(String currentLine) {
        this.typeCheck(currentLine);
        this.symbolCheck(currentLine);
        this.cFieldCheck(currentLine);
    }

    /**
     * Helper function for setting the symbol inside the instruction.
     * Used every time an instruction is read. Changes currentSymbol only if we have an A_INSTRUCTION or L_INSTRUCTION
     *
     * @param currentLine the line that was read
     */
    private void symbolCheck(String currentLine) {
        if (this.currentType == InstructType.A_INSTRUCTION) {
            this.currentSymbol = currentLine.substring(1);
        } else if (this.currentType == InstructType.L_INSTRUCTION) {
            this.currentSymbol = currentLine.substring(1, currentLine.length() - 1);
        } else this.currentSymbol = null;
    }

    /**
     * Helper function for setting the C_Instruction fields.
     * If we have a C_INSTRUCTION, we parse the dest,comp,jump fields and update
     * Otherwise, we reset them all to null.
     *
     * @param currentLine the line that was read
     */
    private void cFieldCheck(String currentLine) {
        if (this.currentType == InstructType.C_INSTRUCTION) {
            if (currentLine.indexOf(';') != -1) {
                String[] instruction = currentLine.split(";");
                this.jump = instruction[1];
                if (currentLine.indexOf('=') != -1) {
                    String[] destComp = instruction[0].split("=");
                    this.dest = destComp[0];
                    this.comp = destComp[1];
                } else {
                    this.dest = "null";
                    this.comp = instruction[0];
                }
            } else {
                String[] destComp = currentLine.split("=");
                this.dest = destComp[0];
                this.comp = destComp[1];
            }
        }
        // reset all parameters if not a c instruction, d
        else {
            this.dest = null;
            this.comp = null;
            this.jump = null;
        }
    }

    public boolean hasMoreLines() throws IOException {
        return this.ready();
    }

    public void advance() throws IOException {
        String currentLine = this.readLine();
        currentLine = currentLine.trim();
        this.paramParser(currentLine);
    }

    public InstructType instructionType() {
        return this.currentType;
    }

    public String symbol() {
        return this.currentSymbol;
    }

    public String getDest() {
        return this.dest;
    }

    public String getComp() {
        return this.comp;
    }

    public String getJump() {
        return this.jump;
    }

    private void resetParam() {
        this.currentType = null;
        this.currentSymbol = null;
        this.dest = null;
        this.comp = null;
        this.jump = null;
    }

    public void resetParser() throws IOException {
        this.reset();
        resetParam();
    }

}
