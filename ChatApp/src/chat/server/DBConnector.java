package chat.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBConnector {

	private final String URL = "jdbc:mysql://10.55.64.72/chat";
	private final String USER = "dmaslev";
	private String password;

	private Connection connection;
//	private Statement statement;
	private ResultSet resultSet;
	private PreparedStatement preparedStatement;

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
//		statement = connection.createStatement();
	}

	public void insert(String sql, Object params[]) throws SQLException {
		preparedStatement = connection.prepareStatement(sql);
		for (int i = 0; i < params.length; i++) {
			preparedStatement.setObject(i + 1, params[i]);
		}
		
		preparedStatement.executeUpdate();
	}

	public ResultSet select(String sql, Object[] params) throws SQLException {
		preparedStatement = connection.prepareStatement(sql);
		for (int i = 0; i < params.length; i++) {
			preparedStatement.setObject(i + 1, params[i]);
		}
		
		resultSet = preparedStatement.executeQuery();
		return resultSet;
	}
}
