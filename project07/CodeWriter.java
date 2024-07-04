import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class CodeWriter extends BufferedWriter {
    private HashMap<String,String> boolCommands = new HashMap<String,String >();
    private HashMap<String,String> arithCommands = new HashMap<String,String >();
    private String[] binaryOps = {"add","sub","and","or"};
    private int boolCount;
    private static final String POP_TMP = "R13";


    public CodeWriter(File file) throws IOException {
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
    }

    private String stackPointer() {
        return "@SP\n";
    }

    private boolean isBinaryArithOp(String command) {
        for (String binaryOp : binaryOps) {
            if (Objects.equals(binaryOp, command)) return true;
        }
        return false;
    }

    private String boolStart(String command){
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
    private String boolTrue(){
        String count = Integer.toString(this.boolCount);
        return "(BOOL" + count + ")\n" +
                setStackHead(-1);
    }
    private String boolFalse(){
        return setStackHead(0) +
                "@ENDBOOL" + Integer.toString(this.boolCount) + "\n" +
                jump();
    }
    private String boolEnd(){
        String end = "(ENDBOOL" + Integer.toString(this.boolCount) + ")" + '\n' +
                incSP();
        this.boolCount++;
        return end;
    }
    /**
     * Helper function
     * Converts an arithmetic command into a series of corresponding hack assembly commands
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
            selection = "@"+Integer.toString(index) + '\n';
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
     * @param segment the memory segment we copy value from
     * @param index the index of element whose value we copy
     * @return push operation
     */
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
     * Creates a series of hack assembly commands that represent the pop operation.
     *
     * @param segment the memory segment we pop into
     * @param index   the index of element in the segment to pop into
     * @return pop operation
     */
    private String pop(String segment, int index) {
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

}
