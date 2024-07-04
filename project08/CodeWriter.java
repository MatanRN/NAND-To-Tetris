import com.sun.tools.jconsole.JConsoleContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class CodeWriter extends BufferedWriter {
    private HashMap<String, String> boolCommands = new HashMap<String, String>();
    private HashMap<String, String> arithCommands = new HashMap<String, String>();
    private String[] binaryOps = {"add", "sub", "and", "or"};
    private int boolCount;
    private int labelCount;
    private String fileName;


    private static final String POP_TMP = "R13";
    private static final String RETURN_TMP = "R14";
    private static final String FRAME_TMP = "R15";


    public CodeWriter(File file, boolean needSysInit) throws IOException {
        super(new FileWriter(file));
        arithCommands.put("add", "M=M+D\n");
        arithCommands.put("sub", "M=M-D\n");
        arithCommands.put("neg", "M=-M\n");
        arithCommands.put("and", "M=M&D\n");
        arithCommands.put("or", "M=M|D\n");
        arithCommands.put("not", "M=!M\n");
        boolCommands.put("eq", "JEQ\n");
        boolCommands.put("gt", "JGT\n");
        boolCommands.put("lt", "JLT\n");
        boolCommands.put("ne", "JNE\n");
        this.boolCount = 0;
        this.labelCount = 0;
        this.fileName = "bootstrap";
        bootstrap(needSysInit);
    }

    /**
     * Performs the bootstrap process
     * initializing the stack pointer and optionally calling the Sys.init function.
     * <p>
     * The bootstrap process sets up the initial state of the virtual machine before executing
     * the provided program. It initializes the stack pointer (SP) to a predefined value and,
     * if required, calls the Sys.init function to start the program execution.
     *
     * @param needSysInit A boolean indicating whether to call the Sys.init function.
     * @throws IOException If an I/O error occurs while writing to the output.
     */
    private void bootstrap(boolean needSysInit) throws IOException {
        String initSP = "//Bootstrap code\n" +
                "@256\n" +
                "D=A\n" +
                stackPointer() +
                "M=D\n";
        this.write(initSP);
        if (needSysInit) {
            writeCall("Sys.init", 0);
        }
        this.newLine();
    }

    private boolean isBinaryArithOp(String command) {
        for (String binaryOp : binaryOps) {
            if (Objects.equals(binaryOp, command)) return true;
        }
        return false;
    }

    private String boolStart(String command) {
        String count = Integer.toString(this.boolCount);
        return description(command) +
                decSP() +
                "A=M\n" +
                "D=M\n" +
                decSP() +
                "A=M\n" +
                "D=M-D\n" +
                "@BOOL" + count + "\n";
    }

    private String boolTrue() {
        String count = Integer.toString(this.boolCount);
        return "(BOOL" + count + ")\n" +
                setStackHead(-1);
    }

    private String boolFalse() {
        return setStackHead(0) +
                "@ENDBOOL" + Integer.toString(this.boolCount) + "\n" +
                jump();
    }

    private String boolEnd() {
        String end = "(ENDBOOL" + Integer.toString(this.boolCount) + ")" + '\n' +
                incSP();
        this.boolCount++;
        return end;
    }

    /**
     * Helper function
     * Converts an arithmetic command into a series of corresponding hack assembly commands
     *
     * @param command arithmetic command
     * @return hack assembly code equivalent of the command
     */
    private String operation(String command) {
        if (arithCommands.containsKey(command)) {
            if (isBinaryArithOp(command)) {
                return description(command) +
                        decSP() +
                        "A=M\n" +
                        "D=M\n" +
                        decSP() +
                        "A=M\n" +
                        arithCommands.get(command) +
                        incSP();
            } else {
                return description(command) +
                        decSP() +
                        "A=M\n" +
                        arithCommands.get(command) +
                        incSP();
            }
        } else {
            //booleanOp
            return boolStart(command) +
                    jump(command) +
                    boolFalse() +
                    boolTrue() +
                    boolEnd();
        }
    }


    /**
     * Converts the VM command into a series of corresponding hack assembly commands
     * Writes said commands into the file
     *
     * @param command command
     * @throws IOException in case of failure in writing
     */
    public void writeOperation(String command) throws IOException {
        String asmCommand = operation(command);
        this.write(asmCommand);
        this.newLine();
    }

    /**
     * Selects the memory address corresponding to a given memory segment and index.
     * <p>
     * The function takes a memory segment and an index as parameters and generates assembly
     * code to calculate the memory address based on the specified segment and index.
     *
     * @param segment The memory segment (e.g., local, argument, this, that, static, pointer, temp, constant).
     * @param index   The index within the memory segment.
     * @return Assembly code representing the address calculation as a single String.
     */
    private String addressSelection(String segment, int index) {
        String convertedSegment = segmentConverter(segment);
        String selection = "";
        if (!segment.equals(convertedSegment)) {
            selection += "@" + convertedSegment + '\n' +
                    "D=M\n" +
                    "@" + Integer.toString(index) + '\n' +
                    "A=D+A\n";
        } else if (segment.equals("static")) {
            selection = "@" + this.fileName + '.' + Integer.toString(index) + '\n';
        } else if (segment.equals("pointer") && index == 0) {
            selection = "@THIS\n";
        } else if (segment.equals("temp")) {
            selection = "@R" + Integer.toString(5 + index) + '\n';
        } else if (segment.equals("pointer") && index == 1) {
            selection = "@THAT\n";
        } else {
            selection = "@" + Integer.toString(index) + '\n';
        }
        return selection;
    }

    /**
     * Creates a series of hack assembly commands that represent the push operation.
     *
     * @param segment the memory segment we copy value from
     * @param index   the index of element whose value we copy
     * @return push operation
     */
    private String push(String segment, int index) {
        String asmSegment = segmentConverter(segment);
        String asmPush = description("push", segment, index) + addressSelection(segment, index);
        if (!segment.equals("constant")) {
            asmPush += "D=M\n";
        } else {
            asmPush += "D=A\n";
        }
        asmPush += stackPointer() +
                "A=M\n" +
                "M=D\n" +
                incSP();

        return asmPush;
    }

    public String push(String label) {
        return description("push", label) +
                "@" + label + "\n" +
                "D=A\n" +
                writeDToCurrentAddressInMemoryOfReg("@SP") +
                incSP();
    }

    /**
     * helper function
     * converts the segment name in a VM command to it's hack assembly label
     *
     * @param segment segment name from a VM command
     * @return string with hack assembly label
     */
    private String segmentConverter(String segment) {
        return switch (segment) {
            case "local" -> "LCL";
            case "argument" -> "ARG";
            case "this" -> "THIS";
            case "that" -> "THAT";
            default -> segment;
        };
    }

    // Helper function that increments the Stack Pointer by 1
    private String incSP() {
        return stackPointer() +
                "M=M+1\n";
    }

    // Helper function that increments the Stack Pointer by 1
    private String decSP() {
        return stackPointer() + "M=M-1\n";
    }

    // Helper function adds the VM command as documentation before the rest of the assembly commands.
    // Used for debugging purposes
    private String description(String command) {
        return "//" + command + "\n";
    }

    private String description(String command, String label) {
        return String.format("\n//%s " + label + " " + '\n',
                command);
    }

    private String description(String command, String asmSegment, int index) {
        return String.format("\n//%s " + asmSegment + " " + Integer.toString(index) + '\n',
                command);
    }

    // Helper function that returns the jump command, according to the VM boolean command
    private String jump(String command) {
        return "D;" + boolCommands.get(command);
    }

    private String jump() {
        return "0;JMP\n";
    }

    /**
     * Helper function that changes the value inside the memory of the stack head to the specified number.
     * Used in boolean commands to set the stack head value to either 0 or -1.
     *
     * @param num The value to set inside the stack head memory. Accepted values: 0 (false) or -1 (true).
     * @return The corresponding Hack Assembly code for setting the stack head value.
     */
    private String setStackHead(int num) {
        return String.format("@SP\nA=M\nM=%s\n", num);
    }

    /**
     * Writes the value currently stored in register D into the memory location pointed to by the address
     * stored in the specified register.
     *
     * @param register The register containing the address of the memory location to be written to.
     * @return The Hack Assembly code for writing the value in register D to the specified memory location.
     */
    private String writeDToCurrentAddressInMemoryOfReg(String register) {
        return register + "\n" +
                "A=M\n" +
                "M=D\n";
    }

    private String pushD() {
        return "@SP\n" +
                "A=M\n" +
                "M=D\n" +
                incSP();
    }


    private String stackPointer() {
        return "@SP\n";
    }


    private String declareLabel(String label) {
        return "(" + label + ")" + "\n";
    }

    /**
     * Initializes the environment for a callee function by generating assembly code
     * to set up the function's local frame, argument segment, and other relevant
     * segments.
     * <p>
     * The function generates assembly code to perform the following actions:
     * 1. Pushes the return label to the stack for later use in handling the return
     * address after the callee function completes its execution.
     * 2. Saves the current values of the LCL, ARG, THIS, and THAT segments onto the stack.
     * 3. Repositions the ARG segment to SP - 5 - nVars, where nVars is the number of local
     * variables in the callee function.
     * 4. Repositions the LCL segment to the current value of the stack pointer (SP).
     * 5. Jumps to the callee function specified by the functionName parameter.
     * 6. Declares and returns a unique return label for the callee function.
     *
     * @param functionName The name of the callee function to jump to.
     * @param nVars        The number of local variables in the callee function.
     * @return Assembly code representing the above operations as a single String.
     */
    private String initCalleeEnv(String functionName, int nVars) {
        String repositionNum = Integer.toString(nVars + 5);
        String returnLabel = declareLabel(this.fileName + "$ret." + this.labelCount);
        this.labelCount++;
        String labelToPush = returnLabel.substring(1, returnLabel.length() - 2);

        String initEnv =
                push(labelToPush);
        String[] segments = {"LCL", "ARG", "THIS", "THAT"};
        for (int i = 0; i < segments.length; i++) {
            initEnv += "@" + segments[i] + '\n' +
                    "D=M\n" +
                    pushD();
        }

        // reposition ARG = SP - 5 -nVars
        String repositionARG =
                stackPointer() +
                        "D=M\n" +
                        "@" + repositionNum + "\n" +
                        "D=D-A\n" +
                        "@ARG\n" +
                        "M=D\n";
        //reposition LCL = SP
        String repositionLCL =
                stackPointer() +
                        "D=M\n" +
                        "@LCL" + "\n" +
                        "M=D\n";

        String gotoFunc = "@" + functionName + "\n" +
                jump();

        return initEnv +
                repositionARG +
                repositionLCL +
                gotoFunc +
                returnLabel;
    }

    private String buildFunction(String functionName, int nVars) {
        String functionLabel = declareLabel(functionName);
        String initLocalSegment = "";
        for (int i = 0; i < nVars; i++) {
            initLocalSegment += push("constant", 0);
        }
        return functionLabel +
                initLocalSegment;
    }

    /**
     * Reinstantiates the calling function's frame and restores the relevant segments
     * after a function call has completed its execution.
     * <p>
     * The function generates assembly code to perform the following actions:
     * 1. Save the current local frame (LCL) to a temporary location.
     * 2. Save the return value to the frame pointer (FRAME_TMP).
     * 3. Pop the return value to the argument (ARG) segment.
     * 4. Restore the stack pointer (SP) to the original caller's argument.
     * 5. Restore the THAT, THIS, ARG, and LCL segments to their original values.
     * 6. Jump to the return address to resume the execution of the calling function.
     *
     * @return Assembly code representing the above operations as a single String.
     */
    private String reInstantiateCaller() {
        String tmpFrame = "@LCL\n" +
                "D=M\n" +
                "@" + RETURN_TMP + '\n' +
                "M=D\n";

        String saveReturnValue = "@5\n" +
                "A=D-A\n" +
                "D=M\n" +
                "@" + FRAME_TMP + '\n' +
                "M=D\n";

        String popReturnToARG = decSP() +
                "A=M\n" +
                "D=M\n" +
                "@ARG\n" +
                "A=M\n" +
                "M=D\n";

        String restoreSP = "@ARG\n" +
                "D=M+1\n" +
                "@SP\n" +
                "M=D\n";

        String restoreTHAT = "@" + RETURN_TMP + '\n' +
                "A=M-1\n" +
                "D=M\n" +
                "@THAT\n" +
                "M=D\n";

        String restoreTHIS = "@2\n" +
                "D=A\n" +
                "@" + RETURN_TMP + '\n' +
                "A=M-D\n" +
                "D=M\n" +
                "@THIS\n" +
                "M=D\n";

        String restoreARG = "@3\n" +
                "D=A\n" +
                "@" + RETURN_TMP + '\n' +
                "A=M-D\n" +
                "D=M\n" +
                "@ARG\n" +
                "M=D\n";

        String restoreLCL = "@4\n" +
                "D=A\n" +
                "@" + RETURN_TMP + '\n' +
                "A=M-D\n" +
                "D=M\n" +
                "@LCL\n" +
                "M=D\n";

        String gotoReturn = "@" + FRAME_TMP + '\n' +
                "A=M\n" +
                jump();

        return tmpFrame +
                saveReturnValue +
                popReturnToARG +
                restoreSP +
                restoreTHAT +
                restoreTHIS +
                restoreARG +
                restoreLCL +
                gotoReturn;
    }

    /**
     * Creates a series of hack assembly commands that represent the pop operation.
     *
     * @param segment the memory segment we pop into
     * @param index   the index of element in the segment to pop into
     * @return pop operation
     */
    private String pop(String segment, int index) {
        String asmSegment = segmentConverter(segment);
        String asmPop = description("pop", segment, index) +
                addressSelection(segment, index);
        asmPop += "D=A\n" +
                "@" + POP_TMP + '\n' +
                "M=D\n" +
                decSP() +
                "A=M\n" +
                "D=M\n" +
                "@" + POP_TMP + '\n' +
                "A=M\n" +
                "M=D\n";

        return asmPop;
    }

    /**
     * Converts the VM push/pop command into a series of corresponding hack assembly commands
     * Writes said commands into the file
     *
     * @param command push/pop command
     * @throws IOException in case of failure in writing
     */
    public void writePushPop(CommandType command, String segment, int index) throws IOException {
        String toWrite = "";
        switch (command) {
            case C_PUSH -> toWrite = push(segment, index);
            case C_POP -> toWrite = pop(segment, index);
        }
        this.write(toWrite);
        this.newLine();
    }

    public void writeLabel(String label) throws IOException {
        String toWrite = declareLabel(this.fileName + '$' + label);
        this.write(toWrite);
        this.newLine();
    }

    public void writeGoTo(String label) throws IOException {
        String toWrite = "// goto " + label + '\n' +
                "@" + this.fileName + '$' + label + "\n" +
                jump();
        this.write(toWrite);
        this.newLine();
    }

    public void writeIf(String label) throws IOException {
        String toWrite = description("if-goto " + label) +
                decSP() +
                "A=M\n" +
                "D=M\n" +
                "@" + this.fileName + '$' + label + "\n" +
                jump("ne");
        this.write(toWrite);
        this.newLine();
    }

    public void writeCall(String functionName, int nVars) throws IOException {
        String toWrite = description("call", functionName, nVars) +
                initCalleeEnv(functionName, nVars);
        this.write(toWrite);
        this.newLine();
    }

    public void writeFunction(String functionName, int nVars) throws IOException {
        String toWrite = description("function", functionName, nVars) +
                buildFunction(functionName, nVars);
        this.write(toWrite);
        this.newLine();
    }

    public void writeReturn() throws IOException {
        String toWrite = description("return") +
                reInstantiateCaller();
        this.write(toWrite);
        this.newLine();
    }

    public void setFileName(String fileName) throws IOException {
        if (this.fileName != fileName) {
            this.write("// END " + this.fileName);
            this.newLine();
            this.fileName = fileName;
        }
        int dotIndex = fileName.lastIndexOf('.');
        this.fileName = fileName.substring(0, dotIndex);
        this.write("// START " + this.fileName + '\n');
        this.labelCount = 0;
    }

}
