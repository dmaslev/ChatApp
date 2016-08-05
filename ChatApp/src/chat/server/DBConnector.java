package chat.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnector {
	
	private final String URL = "jdbc:mysql://10.55.66.175/chat";
	private final String USER = "dmaslev";
	private String password;
	
	private Connection connection;
	private Statement statement;
	private ResultSet resultSet;
	
	public DBConnector(String password) {
		this.password = password;
	}
	
	public void connect() throws SQLException {
		try {
			connection = DriverManager.getConnection(this.URL, this.USER, this.password);
		} catch (SQLException e) {
			throw new SQLException("Unable to connect to " + this.URL + " with user: " + this.USER, e);
		}
		
		System.out.println("Successfully connected to databaserver: " + this.URL);
		statement = connection.createStatement();
	}
	
	public void insert(String username) throws SQLException {
		try {
			statement.execute("INSERT INTO `chat`.`users` (`username`, `password`, `ip`) VALUES ('asd', 'abcd1234', 'hack')");
		} catch (SQLException e) {
			throw new SQLException("", e);
		}
	}
}
