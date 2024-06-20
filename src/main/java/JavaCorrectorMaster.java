import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
public class JavaCorrectorMaster {
    private enum RESULTCODE{
        OK,
        COMPILE_ERROR,
        TEST_ERROR
    }
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
        return new Job("Afortunados", 10);

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
            System.err.println("Failed to create directory: " + e.getMessage());
        }
    }
    private static void runDocker(){

        final Job nextJob = getNextJob();

        if (nextJob == null) return;

        new Thread(()-> {
            final String program = nextJob.getProgram();
            try {
                createDirAndCopyFiles(program, Long.toString(nextJob.getJobID()));
                final Runtime re = Runtime.getRuntime();
                //TODO: De momento no usamos $(pwd) porque no estoy en el directorio que toca
                String c = "docker run --rm -v " + System.getProperty("user.dir") + "/io:/io/ --name codetest codetest " + program + " /io/" + Long.toString(nextJob.getJobID());
                System.out.println(c);
                final Process command = re.exec(c);
                // Wait for the application to Fin
                command.waitFor();

                if (command.exitValue() != 0) {
                    throw new IOException("Failed to execute jar");
                }else{
                    parseResults(Long.toString(nextJob.getJobID()));
                }
            }catch (final IOException | InterruptedException | ParserConfigurationException | SAXException e){
                System.out.println(e.getMessage());
            }
        }).start();
    }
    public static void parseResults(String id) throws IOException, ParserConfigurationException, SAXException {
        Document doc;
        Element root;
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(System.getProperty("user.dir") + "/io/" + id + "/results.xml");
        root = doc.getDocumentElement(); // apuntarà al elemento raíz.
        int resultCode = Integer.parseInt(root.getElementsByTagName("resultcode").item(0).getFirstChild().getNodeValue());
        System.out.println(resultCode);
        // resultCode = RESULTCODE.OK => Correcto
        // resultCode = RESULTCODE.COMPILE_ERROR => No compila. Los errores están en el tag <error>
        // resultCode = RESULTCODE.TEST_ERROR => Error en el test. Los errores están en el tag <error>



    }
}
