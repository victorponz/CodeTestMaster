import java.sql.SQLException;

public class AppService {

    static java.sql.Connection connection;

    public static java.sql.Connection getConnection(){
        ConfigLoader conf = new ConfigLoader();
        String host = conf.getProperty("datasource.url");
        if (connection == null) {
            try {
                connection = java.sql.DriverManager.getConnection(host);
            }catch (SQLException sql){
                System.out.println(sql.getMessage());
                System.exit(0);
            }
        }
        return connection;
    }
}