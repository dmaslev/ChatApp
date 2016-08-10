package chat.server;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import com.mysql.fabric.xmlrpc.base.Data;

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
		DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD hh:mm:ss");

		try {
			while (isServerInputManagerOn) {
				String line = reader.nextLine();
				if (line.equalsIgnoreCase("/help")) {
					printHelpMenu();
				} else if (line.equalsIgnoreCase("/disconnect")) {
					server.stopServer();
				} else if (line.startsWith("/remove: ")) {
					String name = line.substring(8).trim();
					server.disconnectUser(name);
				} else if (line.equalsIgnoreCase("/listall")) {
					server.printConnectedUsers();
				} else if (line.equalsIgnoreCase("/show full history")) {
					System.out.println("Enter start and end date on seperated lines in format YYYY-MM-DD HH:mm:ss "
							+ "or /skip to see full history for selected users.");
					String firstDate = reader.nextLine();
					if (firstDate.equals("/skip")) {
						showFullHistory();	
						continue;
					}
					
					String startDate = reader.nextLine();
					String endDate = reader.nextLine();
					String sql = "SELECT * FROM ("
							+ "SELECT date_logged_in as date, id_user, 'login' as action,  ip "
							+ "FROM connections "
							+ "union "
							+ "SELECT date, recipient, 'message', text "
							+ "FROM  messages  "
							+ "union  "
							+ "SELECT date_logged_out as date, id_user_logout, 'logout', ip  "
							+ "FROM logouts) "
							+ "as result "
							+ "WHERE result.date >= ? AND result.date <= ? ORDER BY date;";
					Object[] params = new Object[] { startDate, endDate };
					
					ResultSet resultSet = server.getDbConnector().select(sql, params);
					printResultSet(resultSet);
				} else if (line.equalsIgnoreCase("/show history")) {
					System.out.print("Enter the usernames of the users that you "
							+ "want to see history for seperated with ',' or 'all' if you want to see info for all users");
					String usersAsString = reader.nextLine();
					String[] users = usersAsString.split("\\s*,\\s*");
					
					System.out.println("Enter start and end date on seperated lines in format YYYY-MM-DD HH:mm:ss "
							+ "or /skip to see full history for selected users.");
					String firstDate = reader.nextLine();
					if (firstDate.equals("/skip")) {
						for (String string : users) {
							// TODO execute the sql query
						}
					}
					
					try {
						Date startDate = dateFormat.parse(firstDate);
						String secondDate = reader.nextLine();
						Date endDate = dateFormat.parse(secondDate);

					} catch (ParseException e) {
						System.err.println("Wrong date format used. " + Logger.printError(e));
					}
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
			reader.close();
		}
	}

	/**
	 * Stops waiting for user input.
	 */
	void shutdown() {
		isServerInputManagerOn = false;
	}

	private void showFullHistory() throws SQLException {
		String sql = "SELECT date_logged_in as date, id_user, 'login' as type,  ip as text FROM connections "
				+ "union SELECT date, recipient, 'message', text FROM  messages union "
				+ "SELECT date_logged_out as date, id_user_logout, 'logout', ip FROM logouts  "
				+ "order by date;";
		Object[] params = new Object[] {};
		try {
			ResultSet resultSet = server.getDbConnector().select(sql, params);
			printResultSet(resultSet);
		} catch (SQLException sqlException) {
			throw new SQLException("Unable to execute the sql query", sqlException);
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
			} else if (type.equals("message")) {
				String message = resultSet.getString("text");
				sBuilder.append(" received message: " + message);
			} else {
				System.out.println("Unknown sql query.");
			}

			sBuilder.append("\n");
		}

		System.out.println(sBuilder.toString());
	}

	/**
	 * Prints information about all supported commands.
	 */
	private void printHelpMenu() {
		// TODO update the info
		System.out.println("- To stop the server enter a command \"/disconnect\". "
				+ "If you want to wait all messages currently in the queue to be sent you can "
				+ "add \"false\" in the command or \"true\" if you want to shut down immediately. "
				+ "If you don't select it all messages will be waited.");
		System.out.println("- To disconnect a user enter a command in format: \"/remove: [username]\".");
		System.out.println("- To see all connected users enter a command \"/listall\".");
		System.out.println("- To see full server history /messages, users connections, "
				+ "users logouts/ enter a command \"/show full history\".");
		System.out.println(" - To see history for concrete user or concrete time period"
				+ " enter a command \"/show history\" and follow the instructions.");
	}

	private void setServer(Server server) {
		this.server = server;
	}
}
