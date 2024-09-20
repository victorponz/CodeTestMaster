import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.FileWriter;
import java.nio.file.DirectoryStream;
import java.sql.PreparedStatement;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class CodeTestMaster {
    private enum RESULTCODE{
        OK,
        COMPILE_ERROR,
        COMPILE_TEST_ERROR,
        TEST_ERROR
    }
    private static ConfigLoader conf;
    private static java.sql.Connection con;
    private static final Logger logger = LogManager.getLogger(CodeTestMaster.class);
    private static final String OUTPUT_DIRECTORY = "io/";
    public static void main(String[] args) throws SQLException {

        /* Va a haber un ciclo continuo
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
        conf = new ConfigLoader();
        con = AppService.getConnection();

        try {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            Runnable task = () -> {
                try {
                    runDocker();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            // Schedule the task to run 100 milliseconds
            executor.scheduleAtFixedRate(task, 0, 100, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }


    }
    public static Job getNextJob() throws SQLException {

        Program program = null;
        Job job = null;
        Statement st = con.createStatement();
        Statement stJob = con.createStatement();


        //Ejecutar la consulta, guardando los datos devueltos en un Resulset
        ResultSet rs = stJob.executeQuery("SELECT * FROM jobs WHERE status = 0 LIMIT 1");
        if (rs.next()){
            ResultSet rsProgram = st.executeQuery("SELECT * FROM programs WHERE id = " + rs.getLong("id_program"));;
            if (rsProgram.next()){
                program = new Program(rsProgram.getInt("id"),
                        rsProgram.getString("class_name"),
                        rsProgram.getString(
                                "source_code"),
                        rsProgram.getString("source_code_test"));
                job = new Job(rs.getLong("id"), rs.getString("source_code"), program);
            }
        }
        rs.close();
        return job;
    }
    private static void createDirAndCopyFiles(Job job) throws IOException{
        String dirName = OUTPUT_DIRECTORY + job.getId();
        String className = job.getProgram().getClassName();
        FileWriter fw = null;
     	Path path = Paths.get(System.getProperty("user.dir") + "/" + dirName);
        try {
            // Create the directory
            if (Files.exists(path) && Files.isDirectory(path)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path entry : stream) {
                        if (Files.isRegularFile(entry)) {
                            Files.delete(entry);
                        }
                    }
                }
                logger.info("JobId " + job.getId() +  " - Directory exists. Contents removed");
                System.out.println("Directory exists. Contents removed");
            } else {
                Files.createDirectory(path);
                logger.info("JobId " + job.getId() +  " - Directory created successfully");
                System.out.println("Directory created successfully!");
            }

            fw = new FileWriter(System.getProperty("user.dir") + "/" + dirName + "/" +  className + ".java");
            fw.write(job.getSourceCode());
            fw.close();
            fw = new FileWriter(System.getProperty("user.dir") + "/" + dirName + "/" +  className + "Test.java");
            fw.write(job.getProgram().getSourceCodeTest());
            fw.close();
            //Path source = Paths.get(System.getProperty("user.dir") + "/" + className + ".java");
            //Path target = Paths.get(System.getProperty("user.dir") + "/io/" + dirName + "/" +  className + ".java");
            //Files.copy(source, target);
           	
            //source = Paths.get(System.getProperty("user.dir") + "/" + className + "Test.java");
            //target = Paths.get(System.getProperty("user.dir") + "/io/" + dirName + "/" +  className + "Test.java");
            //Files.copy(source, target);
        } catch (IOException e) {
            // Handle the error
            logger.error("JobId " + job.getId() +  " - Failed to create directory: " + e.getMessage());
            System.err.println("Failed to create directory: " + e.getMessage());
        }finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error("JobId " + job.getId() +  " - General Error " + e.getMessage());
                    System.out.println(e.getMessage());
                }
            }
        }
    }
    private static void runDocker() throws SQLException, IOException, InterruptedException, ParserConfigurationException, SAXException {

        final Job nextJob = getNextJob();

        if (nextJob == null) return;
        final String program = nextJob.getProgram().getClassName();
        updateJobInProgress(nextJob.getId());
        createDirAndCopyFiles(nextJob);
        final Runtime re = Runtime.getRuntime();
        //TODO: De momento no usamos $(pwd) porque no estoy en el directorio que toca
        String c = "docker run --rm -v " + System.getProperty("user.dir") + "/" + OUTPUT_DIRECTORY +  nextJob.getId() + ":/"
                + OUTPUT_DIRECTORY +  nextJob.getId() + "/ --name codetestrunner codetestrunner " + program + " /" + OUTPUT_DIRECTORY + nextJob.getId();
        final Process command = re.exec(c);
        // Wait for the application to Finish
        command.waitFor();

        if (command.exitValue() != 0) {
            logger.error("JobId " + nextJob.getId() +  " - to execute jar");
            throw new IOException("Failed to execute jar");
        }else{
            parseResults(Long.toString(nextJob.getId()));
        }
        logger.info("JobId " + nextJob.getId() + " - ended successfully");

    }
    public static  void parseResults(String id) throws IOException, ParserConfigurationException, SAXException, SQLException {
        Document doc;
        Element root;
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(System.getProperty("user.dir") + "/" + id + "/results.xml");
        root = doc.getDocumentElement(); // apuntarà al elemento raíz.
        int resultCodeInt = Integer.parseInt(root.getElementsByTagName("resultcode").item(0).getFirstChild().getNodeValue());
        System.out.println(resultCodeInt);
        RESULTCODE resultCode = RESULTCODE.values()[resultCodeInt];
        String error = "";
        if (root.getElementsByTagName("error").getLength() > 0)
            error = root.getElementsByTagName("error").item(0).getFirstChild().getNodeValue();

        // resultCode = RESULTCODE.OK => Correcto
        // resultCode = RESULTCODE.COMPILE_ERROR => No compila. Los errores están en el tag <error>
        // resultcode = RESULTCODE.COMPILE_TEST_ERROR => No compila el test. Los errores están en el tag <error>
        // resultCode = RESULTCODE.TEST_ERROR => Error en el test. Los errores están en el tag <error>
        updateJob(Long.parseLong(id), resultCodeInt, error);

    }
    private static void updateJob(long id, int resultCode, String error) throws SQLException {
        ResultSet rs;
        PreparedStatement st = null;
        String query = "UPDATE jobs SET status = 2, result_code = ?, error = ? WHERE id = ?";
        st = con.prepareStatement(query);
        st.setInt(1, resultCode);
        st.setString(2, error);
        st.setLong(3, id);

        st.executeUpdate();
    }
    private static void updateJobInProgress(long id) throws SQLException {
        ResultSet rs;
        PreparedStatement st = null;
        String query = "UPDATE jobs SET status = 1 WHERE id = ?";
        st = con.prepareStatement(query);
        st.setLong(1, id);

        st.executeUpdate();
    }
}
