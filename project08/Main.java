import java.io.File;

import java.io.IOException;

public class Main {
    /**
     * Iterates over a file, parsing each line and writing the equivalent hack assembly commands into the writer
     * @param parser
     * @param writer
     * @throws IOException
     */
    public static void readWriteFile(Parser parser, CodeWriter writer) throws IOException {
        while(parser.hasMoreLines()){
            parser.advance();
            switch (parser.commandType()){
                case C_POP,C_PUSH -> writer.writePushPop(parser.commandType(), parser.arg1(), parser.arg2());
                case C_ARITHMETIC -> writer.writeOperation(parser.arg1()); //arg1 holds the command if arithmetic
                case C_LABEL ->  writer.writeLabel(parser.arg1());
                case C_GOTO -> writer.writeGoTo(parser.arg1());
                case C_IF ->  writer.writeIf(parser.arg1());
                case C_CALL -> writer.writeCall(parser.arg1(), parser.arg2());
                case C_FUNCTION -> writer.writeFunction(parser.arg1(), parser.arg2());
                case C_RETURN -> writer.writeReturn();
            }
        }
    }
    private static boolean containSys(File[] files){
        for (File file:files) {
            if(file.getName().equals("Sys.vm")) return true;
        }
        return false;
    }

    /**
     * Constructs the path of the file we write into.
     * For a single file, returns an identical path with the .asm suffix
     * For directory, returns a path into the directory, ending with directory_name.asm
     * Naming is the same, except for the .asm suffix.
     * @param f
     * @param mode 0 for single file, 1 for directory
     * @return
     */
    public static String createPath(File f, int mode){
        if(mode == 1){
            String path = f.getPath();
            String name = f.getName();
            return path+'/' + name + ".asm";
        }
        else{
            String path = f.getPath();
            int dotIndex = path.lastIndexOf('.');
            return path.substring(0,dotIndex) + ".asm";
        }
    }
    public static void main(String[] args) throws IOException {
        String path = args[0]; //get user input for .vm file or directory
        File f = new File(path);
        boolean needSysInit = false;

        // if Assembler + dir is provided translate all asm in the folder
        if (f.isDirectory()) {
            File[] directory = f.listFiles((dir, name) -> name.toLowerCase().endsWith(".vm"));
            File dirASM = new File(createPath(f,1));
            if((directory.length>1)|| containSys(directory)){
                needSysInit = true;
            }
            CodeWriter hackASM = new CodeWriter(dirASM,needSysInit);
            for (File file : directory) {
                Parser vmParser = new Parser(file);
                hackASM.setFileName(file.getName());
                readWriteFile(vmParser,hackASM);
                vmParser.close();
            }
            hackASM.close();
        }
        //operate on a single file
        else {
            File fileASM = new File(createPath(f,0));
            if(containSys(fileASM.listFiles())){
                needSysInit = true;
            }
            CodeWriter hackASM = new CodeWriter(fileASM,needSysInit);
            Parser vmParser = new Parser(f);
            readWriteFile(vmParser,hackASM);
            vmParser.close();
            hackASM.close();
        }
    }
}
