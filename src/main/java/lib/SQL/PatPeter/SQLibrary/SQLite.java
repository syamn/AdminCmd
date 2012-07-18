/**
 * SQLite
 * Inherited subclass for reading and writing to and from an SQLite file.
 * 
 * Date Created: 2011-08-26 19:08
 * @author PatPeter
 */
package lib.SQL.PatPeter.SQLibrary;

/*
 * SQLite
 */
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import lib.SQL.PatPeter.SQLibrary.DatabaseConfig.DatabaseType;

public class SQLite extends Database {
	public String location;
	public String name;
	private final File sqlFile;

	public SQLite(final Logger log, final String prefix, final String name,
			final String location) {
		super(log, prefix, "[SQLite] ");
		this.name = name;
		this.location = location;
		final File folder = new File(this.location);
		if (this.name.contains("/") || this.name.contains("\\")
				|| this.name.endsWith(".db")) {
			this.writeError("The database name cannot contain: /, \\, or .db",
					true);
		}
		if (!folder.exists()) {
			folder.mkdir();
		}

		sqlFile = new File(folder, name + ".db");
	}

	@Override
	protected void initialize() throws SQLException {
		try {
			Class.forName("org.sqlite.JDBC");
			if (!sqlFile.exists()) {
				try {
					sqlFile.createNewFile();
				} catch (final IOException e) {}
			}
		} catch (final ClassNotFoundException e) {
			throw new SQLException("Can't load JDBC Driver", e);
		}
	}

	@Override
	public void open() throws SQLException {
		initialize();
		this.connection = DriverManager.getConnection("jdbc:sqlite:"
				+ sqlFile.getAbsolutePath());

	}

	@Override
	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (final SQLException ex) {
				this.writeError("SQL exception in close(): " + ex, true);
			}
		}
	}

	@Override
	public Connection getConnection() {
		return this.connection;
	}

	@Override
	public boolean checkConnection() {
		if (connection != null) {
			return true;
		}
		return false;
	}

	@Override
	public ResultSet query(final String query) {
		Statement statement = null;
		ResultSet result = null;

		try {
			statement = connection.createStatement();
			result = statement.executeQuery("SELECT date('now')");

			switch (this.getStatement(query)) {
				case SELECT :
					result = statement.executeQuery(query);
					break;

				case INSERT :
				case UPDATE :
				case DELETE :
				case CREATE :
				case ALTER :
				case DROP :
				case TRUNCATE :
				case RENAME :
				case DO :
				case REPLACE :
				case LOAD :
				case HANDLER :
				case CALL :
					this.lastUpdate = statement.executeUpdate(query);
					break;

				default :
					result = statement.executeQuery(query);

			}
			return result;
		} catch (final SQLException e) {
			if (e.getMessage().toLowerCase().contains("locking")
					|| e.getMessage().toLowerCase().contains("locked")) {
				return retry(query);
			} else {
				this.writeError("SQL exception in query(): " + e.getMessage(),
						false);
			}

		}
		return null;
	}

	@Override
	public PreparedStatement prepare(final String query) {
		try {
			final PreparedStatement ps = connection.prepareStatement(query);
			return ps;
		} catch (final SQLException e) {
			if (!e.toString().contains("not return ResultSet")) {
				this.writeError(
						"SQL exception in prepare(): " + e.getMessage(), false);
			}
		}
		return null;
	}

	@Override
	public boolean createTable(final String query) {
		Statement statement = null;
		try {
			if (query.equals("") || query == null) {
				this.writeError(
						"Parameter 'query' empty or null in createTable().",
						true);
				return false;
			}

			statement = connection.createStatement();
			statement.execute(query);
			return true;
		} catch (final SQLException ex) {
			this.writeError(ex.getMessage(), true);
			return false;
		}
	}

	@Override
	public boolean checkTable(final String table) {
		DatabaseMetaData dbm = null;
		try {
			dbm = this.connection.getMetaData();
			final ResultSet tables = dbm.getTables(null, null, table, null);
			if (tables.next()) {
				return true;
			} else {
				return false;
			}
		} catch (final SQLException e) {
			this.writeError("Failed to check if table \"" + table
					+ "\" exists: " + e.getMessage(), true);
			return false;
		}
	}

	@Override
	public boolean wipeTable(final String table) {
		Statement statement = null;
		String query = null;
		try {
			if (!this.checkTable(table)) {
				this.writeError("Table \"" + table
						+ "\" in wipeTable() does not exist.", true);
				return false;
			}
			statement = connection.createStatement();
			query = "DELETE FROM " + table + ";";
			statement.executeQuery(query);
			return true;
		} catch (final SQLException ex) {
			if (!(ex.getMessage().toLowerCase().contains("locking") || ex
					.getMessage().toLowerCase().contains("locked"))
					&& !ex.toString().contains("not return ResultSet")) {
				this.writeError("Error at SQL Wipe Table Query: " + ex, false);
			}
			return false;
		}
	}

	/*
	 * <b>retry</b><br> <br> Retries a statement and returns a ResultSet. <br>
	 * <br>
	 * 
	 * @param query The SQL query to retry.
	 * 
	 * @return The SQL query result.
	 */
	public ResultSet retry(final String query) {
		Statement statement = null;
		ResultSet result = null;

		try {
			statement = connection.createStatement();
			result = statement.executeQuery(query);
			return result;
		} catch (final SQLException ex) {
			if (ex.getMessage().toLowerCase().contains("locking")
					|| ex.getMessage().toLowerCase().contains("locked")) {
				this.writeError(
						"Please close your previous ResultSet to run the query: \n\t"
								+ query, false);
			} else {
				this.writeError("SQL exception in retry(): " + ex.getMessage(),
						false);
			}
		}

		return null;
	}
	/*
	 * (Non javadoc)
	 * 
	 * @see lib.SQL.PatPeter.SQLibrary.Database#getType()
	 */
	@Override
	public DatabaseType getType() {
		return DatabaseType.SQLITE;
	}
}