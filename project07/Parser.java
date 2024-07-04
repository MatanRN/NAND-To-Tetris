import java.io.*;

public class Parser extends BufferedReader {
    private CommandType type;
    private String arg1; //sets to the command itself in case we have no args
    private int arg2; //sets to -1 if current command has no arg2
    public Parser(File file) throws FileNotFoundException {
        super(new FileReader(file));
        this.type = null;
        this.arg1 = null;
        this.arg2 = -1;
    }

    public boolean hasMoreLines() throws IOException {
        return this.ready();
    }

    /**
     * Helper function
     * Processes the array generated by splitting the entire VM command
     * Updates type,arg1,arg2 accordingly
     * @param parts array generated by splitting the entire VM command
     */
    private void parse(String[] parts){
        switch (parts[0]) {
            //push/pop commands
            case "push" -> {
                this.type = CommandType.C_PUSH;
                this.arg1 = parts[1]; //segment
                this.arg2 = Integer.parseInt(parts[2]); //i
            }
            case "pop" -> {
                this.type = CommandType.C_POP;
                this.arg1 = parts[1]; //segment
                this.arg2 = Integer.parseInt(parts[2]); //i
            }
            //arithmetic commands
            case "add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not" -> {
                this.type = CommandType.C_ARITHMETIC;
                this.arg1 = parts[0]; // in arithmetic commands we save the command here
                this.arg2 = -1;
            }
            //branching commands
            case "label" -> {
                this.type = CommandType.C_LABEL;
                this.arg1 = parts[1] ;//symbol
                this.arg2 = -1;
            }
            case "goto" -> {
                this.type = CommandType.C_GOTO;
                this.arg1 = parts[1] ;//symbol
                this.arg2 = -1;
            }
            case "if-goto" -> {
                this.type = CommandType.C_IF;
                this.arg1 = parts[1] ;//symbol
                this.arg2 = -1;
            }
            //function commands
            case "function" ->{
                this.type = CommandType.C_FUNCTION;
                this.arg1 = parts[1]; //symbol
                this.arg2 = Integer.parseInt(parts[2]); //n
            }
            case "call" ->{
                this.type = CommandType.C_CALL;
                this.arg1 = parts[1]; //symbol
                this.arg2 = Integer.parseInt(parts[2]); //n
            }
            case "return" ->{
                    this.type = CommandType.C_RETURN;
                    this.arg1 = parts[0]; //saves the return command
                    this.arg2 = -1;
            }
        }
    }

    /**
     * Reads the next command from the file, and parses it
     * @throws IOException in case of reading failure
     */
    public void advance() throws IOException {
        if(this.hasMoreLines()){
            String currCommand = this.readLine();
            if(currCommand.isEmpty()){
                this.type = CommandType.IGNORE_WHITESPACE;
                return;
            }
            if(currCommand.charAt(0)=='/'){
                this.type = CommandType.IGNORE_DOCUMENTATION;
                return;
            }
            String[] parts = currCommand.split(" "); //array of the command parts
            parse(parts);
        }
    }
    public CommandType commandType(){
        return this.type;
    }
    public String arg1(){
        // in arithmetic commands we save the command here
        return this.arg1;
    }
    public int arg2(){
        return this.arg2;
    }

}
