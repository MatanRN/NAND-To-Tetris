import java.io.File;
import java.io.IOException;

public class Main {
    public static void directoryHandler(File[] directory) {
        if(directory!=null && directory.length>0){
            for (File file : directory) {
                try {
                    singleFileHandler(file);
                } catch (IOException e) {
                    System.out.println("Error processing file " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public static void singleFileHandler(File file) throws IOException {
        String filePath = file.getPath();
        int dotIndex = filePath.lastIndexOf('.');
        if(dotIndex != -1 && filePath.substring(dotIndex).equals(".asm")){
            HackAssembler hackASM = new HackAssembler(file);
            hackASM.firstPass();
            hackASM.secondPass(filePath.substring(0, dotIndex)); 
        }
    }

    public static void main(String[] args) {
        String path = args[0]; //get user input for Assembler + filepath
        try {
            File f = new File(path);
            if (!f.exists()) {
                throw new IOException("File not found");
            }
            if (f.isDirectory()) {
                directoryHandler(f.listFiles());
            }
            else{
                singleFileHandler(f);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}