import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
public class JavaCorrectorMaster{
    public static void main(String[] args) {

        /*Va a haber un ciclo continuo
        Si hay algo que hacer:
        1.- Se crea un hilo
        2.- Se ejecuta un docker 
        3.- Se parsea la salida de este docker    
        */
        // ProcessBuilder pb = new ProcessBuilder("docker", "run", "--rm", "myimage");
        // pb.redirectErrorStream(true);
        // Process p = pb.start();
        // InputStream is = p.getInputStream();
        // BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        // String line;
        // while ((line = reader.readLine()) != null) {
        //     System.out.println(line);
        // }
        // p.waitFor();
        int nextJobIndex = 0;
        do{
            runDocker();
            nextJobIndex++;
        }while(false);
        
    }
    public static Job getNextJob() {
        //TODO: De momento lo hacemos ficticios
        return new Job("Afortunados", 9);

    }
    private static void createDirAndCopyFiles(String className, String dirName) throws IOException{
     	Path path = Paths.get(System.getProperty("user.dir") + "/io/" + dirName);
        try {
            // Create the directory
            Files.createDirectory(path);
            System.out.println("Directory created successfully!");
            Path source = Paths.get(System.getProperty("user.dir") + "/" + className + ".java");
            Path target = Paths.get(System.getProperty("user.dir") + "/io/" + dirName + "/" +  className + ".java");
            Files.copy(source, target);
           	
            source = Paths.get(System.getProperty("user.dir") + "/" + className + "Test.java");
            target = Paths.get(System.getProperty("user.dir") + "/io/" + dirName + "/" +  className + "Test.java");
            Files.copy(source, target);
        } catch (IOException e) {
            // Handle the error
            System.err.println("Failed to create directory2: " + e.getMessage());
        }
    }
    private static void runDocker(){

        try {
            final Job nextJob = getNextJob();

            if (nextJob == null) return;


            final String program = nextJob.getProgram();
            createDirAndCopyFiles(program, Long.toString(nextJob.getJobID()));
            final Runtime re = Runtime.getRuntime();
            //TODO: De momento no usamos $(pwd) porque no estoy en el directorio que toca
            //final Process command = re.exec("docker run --rm -v /home/victorponz/Documentos/repos/JavaCorrector/io:/application/io --name javacorrector victorponz/javacorrector:v0 " + program + " job" + nextJob.getJobID());
            System.out.println("docker run --rm -v " + System.getProperty("user.dir") + "/io:/io/ --name codetest codetest " + program  + " " + Long.toString(nextJob.getJobID()));
            final Process command = re.exec("docker run --rm -v " + System.getProperty("user.dir") + "/io:/io/ --name codetest codetest " + program  + " /io/" + Long.toString(nextJob.getJobID()));
            // Wait for the application to Fin
            command.waitFor();


            if (command.exitValue()!= 0) {
                throw new IOException("Failed to execute jar, " );
            }

        } catch (final IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}
