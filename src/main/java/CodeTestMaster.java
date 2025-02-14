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

import java.util.concurrent.*;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class CodeTestMaster {
    private enum RESULTCODE{
        OK(0),
        COMPILE_ERROR(1),
        COMPILE_TEST_ERROR(2),
        TEST_ERROR(3),
        TIMEOUT_ERROR(4),
        FATAL_ERROR(5);

        private final int code;

        // Constructor to set the integer value
        RESULTCODE(int code) {
            this.code = code;
        }

        // Getter to retrieve the integer value
        public int getCode() {
            return code;
        }
    }

    private static java.sql.Connection con;
    private static final Logger logger = LogManager.getLogger(CodeTestMaster.class);
    private static final String OUTPUT_DIRECTORY = "io/";

    public static void main(String[] args) {
        ConfigLoader conf = new ConfigLoader();
        con = AppService.getConnection();
        // Create a scheduled executor service
        //TODO Configurable corePoolSize
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

        // Schedule a task to run periodically (every 1 second)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ExecutorService singleTaskExecutor = Executors.newSingleThreadExecutor();
                Future<Job> future = singleTaskExecutor.submit(() -> {
                    Job job = null;

                    try {
                        // Step 1: Get the next job
                        job = getNextJob();

                        if (job == null) {
                            System.out.println("No job available.");
                            return null; // Return null if no job is found
                        }
                        //System.out.println("Processing job with id " + job.getId());
                        logger.info("Processing job with id " + job.getId());

                        // Step 2: Process the job (run Docker)
                        runDocker(job);

                        // Step 3: Return the job after successful execution
                        return job;

                    } catch (SQLException e) {
                        logger.error("JobId " + job.getId() +  " - Database error while processing the job " + e.getMessage());
                        return job;
                    } catch (InterruptedException e) {
                        if (job != null) {
                            try {
                                updateJob(job.getId(), RESULTCODE.TIMEOUT_ERROR.getCode(), "Timeout!");
                            } catch (SQLException ex) {
                                logger.error("JobId " + job.getId() +  " - failed to update job with timeout error " + ex.getMessage());
                            }
                            return job;
                        }
                        return null;
                    } catch (Exception e) {
                        if (job != null) {
                            try {
                                updateJob(job.getId(), RESULTCODE.FATAL_ERROR.getCode(), e.getMessage());
                            } catch (SQLException ex) {
                                logger.error("JobId " + job.getId() +  " - failed to update job with sql error " + ex.getMessage());
                                throw new RuntimeException(ex);
                            }
                        }
                        return null;
                    }
                });

                Job job = null;
                try {
                    // Set a time limit for the task to complete
                    //TODO timeout configurable
                    job = future.get(5, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    if (job != null) {
                        try {
                            updateJob(job.getId(), RESULTCODE.TIMEOUT_ERROR.getCode(), "Task timed out!");
                        } catch (SQLException ex) {
                            logger.error("JobId " + job.getId() +  " - failed to update job with SQL error " + ex.getMessage());
                        }
                    }
                    future.cancel(true); // Cancel the task if it exceeds the time limit
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("JobId " + job.getId() +  " - Error during task execution: " + e.getMessage());
                } finally {
                    singleTaskExecutor.shutdown();
                }
            } catch (Exception e) {
                logger.error("Exception in scheduled task: " + e.getMessage());

            }
        }, 0, 1, TimeUnit.SECONDS);

    }

    public static  Job getNextJob() throws SQLException {

        Program program;
        Job job = null;
        Statement st = con.createStatement();
        Statement stJob = con.createStatement();


        //Ejecutar la consulta, guardando los datos devueltos en un Resulset
        ResultSet rs = stJob.executeQuery("SELECT * FROM jobs WHERE status = 0 LIMIT 1");
        if (rs.next()){
            ResultSet rsProgram = st.executeQuery("SELECT * FROM programs WHERE id = " + rs.getLong("id_program"));
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
                }catch (IOException ioe){
                    logger.error("JobId " + job.getId() +  " - Error creating directory");
                    throw new IOException("JobId " + job.getId() +  " - Error creating directory");
                }
                logger.info("JobId " + job.getId() +  " - Directory exists. Contents removed");
                //System.out.println("Directory exists. Contents removed");
            } else {
                Files.createDirectory(path);
                logger.info("JobId " + job.getId() +  " - Directory created successfully");
            }

            //Write the sourcecode
            fw = new FileWriter(System.getProperty("user.dir") + "/" + dirName + "/" +  className + ".java");
            fw.write(job.getSourceCode());
            fw.close();
            //write the test unit
            fw = new FileWriter(System.getProperty("user.dir") + "/" + dirName + "/" +  className + "Test.java");
            fw.write(job.getProgram().getSourceCodeTest());
            fw.close();

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
    private static void runDocker(Job nextJob) throws SQLException, IOException, InterruptedException, ParserConfigurationException, SAXException {

        if (nextJob == null) return;
        //Get the classname of the program to run needed by codetestrunner docker
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
            logger.error("JobId " + nextJob.getId() +  " - Failed to execute jar");
            throw new IOException("Failed to execute jar");
        }else{
            parseResults(Long.toString(nextJob.getId()));
        }
        logger.info("JobId " + nextJob.getId() + " - ended successfully");

    }
    public static  void parseResults(String id) throws IOException, ParserConfigurationException, SAXException, SQLException {
        Document doc;
        Element root;

        //The results are located in a file called results.xml generated by codetestrunner docker
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(System.getProperty("user.dir") + "/" + OUTPUT_DIRECTORY + id + "/results.xml");
        root = doc.getDocumentElement(); // apuntarà al elemento raíz.
        int resultCodeInt = Integer.parseInt(root.getElementsByTagName("resultcode").item(0).getFirstChild().getNodeValue());

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
        PreparedStatement st;
        String query = "UPDATE jobs SET status = 2, result_code = ?, error = ? WHERE id = ?";
        st = con.prepareStatement(query);
        st.setInt(1, resultCode);
        st.setString(2, error);
        st.setLong(3, id);

        st.executeUpdate();
    }
    private static void updateJobInProgress(long id) throws SQLException {
        PreparedStatement st;
        String query = "UPDATE jobs SET status = 1 WHERE id = ?";
        st = con.prepareStatement(query);
        st.setLong(1, id);

        st.executeUpdate();
    }
}
