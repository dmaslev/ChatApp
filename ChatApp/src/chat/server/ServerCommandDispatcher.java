package chat.server;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Scanner;

import chat.util.Logger;

public class ServerCommandDispatcher extends Thread {

	private Server server;
	private boolean isServerInputManagerOn;
	private Scanner reader;

	public ServerCommandDispatcher(Server server, Scanner reader) {
		this.setServer(server);
		this.reader = reader;
	}

	/**
	 * Listens for input commands and executes them.
	 */
	public void run() {
		isServerInputManagerOn = true;

		try {
			while (isServerInputManagerOn) {
				String line = reader.nextLine();
				if (line.equalsIgnoreCase("/help")) {
					printHelpMenu();
				} else if (line.equalsIgnoreCase("/disconnect")) {
					server.stopServer();
				} else if (line.startsWith("/remove:")) {
					String name = line.substring(8).trim();
					server.disconnectUser(name);
				} else if (line.equalsIgnoreCase("/listall")) {
					server.printConnectedUsers();
				} else if (line.equalsIgnoreCase("/show full history")) {
					System.out.println("Enter start and end date on seperated lines in format YYYY-MM-DD HH:mm:ss "
							+ "or /all to see full history for selected users.");
					String startDate = reader.nextLine();
					if (startDate.equals("/all")) {
						showFullHistory();
						continue;
					}

					showFullHistoryTimePeriod(startDate);
				} else if (line.equalsIgnoreCase("/show history")) {
					System.out.println("Enter the usernames of the users that you "
							+ "want to see history for seperated with ',': ");
					String usersAsString = reader.nextLine();

					showHistory(usersAsString);
				} else {
					System.out.println("Invalid command.");
				}
			}
		} catch (IOException e) {
			// Closing server side listeners resources failed.
			System.err.println("Closing server side listeners resources failed. " + Logger.printError(e));
		} catch (SQLException e) {
			System.err.println("Lost connection with the database. " + Logger.printError(e));
		} finally {
			shutdown();
		}
	}

	/**
	 * Stops waiting for user input.
	 */
	void shutdown() {
		isServerInputManagerOn = false;
		reader.close();
	}

	private void showFullHistoryTimePeriod(String startDate) throws SQLException {
		String endDate = reader.nextLine();
		
		String sql = "SELECT * FROM (SELECT date_logged_in as date, id_user, 'login' as type, ip as text "
				+ "FROM connections union SELECT date as date, recipient, 'received' as type, text "
				+ "FROM  messages  UNION SELECT date, recipient, 'sent', text FROM messages union  "
				+ "SELECT date_logged_out as date, id_user_logout, 'logout', ip  FROM logouts) as result "
				+ "WHERE result.date >= ? AND result.date <= ? ORDER BY date;";
		String[] params = new String[] { startDate, endDate };

		try {
			ResultSet resultSet = server.getDbConnector().select(sql, params);
			printResultSet(resultSet);
		} catch (SQLException e) {
			throw new SQLException("Unable to execute the sql query: " + sql, e);
		}
	}

	private void showFullHistory() throws SQLException {
		String sql = "SELECT date_logged_in as date, id_user, 'login' as type,  ip as text FROM connections "
				+ "UNION SELECT date, recipient, 'sent', text FROM  messages "
				+ "UNION SELECT date, sender, 'received', text FROM messages "
				+ "UNION SELECT date_logged_out as date, id_user_logout, 'logout', ip FROM logouts ORDER BY date";
		String[] params = new String[] {};
		try {
			ResultSet resultSet = server.getDbConnector().select(sql, params);
			printResultSet(resultSet);
		} catch (SQLException sqlException) {
			throw new SQLException("Unable to execute the sql query: " + sql, sqlException);
		}
	}

	private void printResultSet(ResultSet resultSet) throws SQLException {
		StringBuilder sBuilder = new StringBuilder();

		while (resultSet.next()) {

			Date date = resultSet.getDate("date");
			Date time = resultSet.getTime("date");
			int userID = resultSet.getInt("id_user");
			String userSQL = "SELECT username FROM users WHERE id_users =?";
			Object[] params = new Object[] { userID };
			ResultSet user = server.getDbConnector().select(userSQL, params);
			if (user.next()) {
				String username = user.getString("username");
				sBuilder.append(date + " " + time + " " + ": " + username);
			}

			String type = resultSet.getString("type");
			if (type.equals("login")) {
				String ip = resultSet.getString("text");
				sBuilder.append(" logged in from: " + ip);
			} else if (type.equals("logout")) {
				String ip = resultSet.getString("text");
				sBuilder.append(" logged out from: " + ip);
			} else if (type.equals("received")) {
				String message = resultSet.getString("text");
				sBuilder.append(" received message: " + message);
			} else if (type.equals("sent")) {
				String message = resultSet.getString("text");
				sBuilder.append(" sent message: " + message);
			} else {
				System.out.println("Unknown sql query.");
			}

			sBuilder.append("\n");
		}

		if (sBuilder.length() == 0) {
			System.out.println("No information found. ");
			return;
		}

		System.out.println(sBuilder.toString());
	}

	private void showHistory(String usersAsString) throws SQLException {
		String[] users = usersAsString.split("\\s*,\\s*");

		String sql = "SELECT * FROM "
				+ "(SELECT date_logged_in AS date, id_user, 'login' AS type, ip AS text FROM connections "
				+ "UNION SELECT  date, sender, 'sent', text FROM messages "
				+ "UNION SELECT date, recipient, 'received', text FROM messages "
				+ "UNION SELECT date_logged_out AS date, id_user_logout, 'logout', ip FROM logouts) "
				+ "AS result JOIN users u on result.id_user = u.id_users WHERE ";

		StringBuilder sBuilder = new StringBuilder();
		boolean isFirstElement = true;
		for (String user : users) {
			if (!isFirstElement) {
				sBuilder.append(" OR ");
			}

			sBuilder.append("u.username = '" + user + "' ");

			isFirstElement = false;
		}
		
		System.out.println("Enter start and end date on seperated lines in format YYYY-MM-DD HH:mm:ss "
				+ "or /all to see full history for selected users.");
		String firstDate = reader.nextLine();
		Object[] params = new Object[] {};
		if (!firstDate.equals("/all")) {
			String secondDate = reader.nextLine();
			if (sBuilder.length() != 0) {
				sBuilder.append(" AND ");
			}

			sBuilder.append(" result.date >= ? AND result.date <= ? ");
			params = new Object[] { firstDate, secondDate };
		}

		sql = sql + sBuilder.toString() + " ORDER BY date";
		ResultSet resultSet = server.getDbConnector().select(sql, params);
		printResultSet(resultSet);
	}

	/**
	 * Prints information about all supported commands.
	 */
	private void printHelpMenu() {
		System.out.println("- To stop the server enter a command \"/disconnect\". "
				+ "If you want to wait all messages currently in the queue to be sent you can "
				+ "add \"false\" in the command or \"true\" if you want to shut down immediately. "
				+ "If you don't select it all messages will be waited.");
		System.out.println("- To disconnect a user enter a command in format: \"/remove: [username]\".");
		System.out.println("- To see all connected users enter a command \"/listall\".");
		System.out.println("- To see full server history /messages, users connections, "
				+ "users logouts/ enter a command \"/show full history\" and follow the instructions.");
		System.out.println(" - To see history for concrete user or concrete time period"
				+ " enter a command \"/show history\" and follow the instructions.");
	}

	private void setServer(Server server) {
		this.server = server;
	}
}
