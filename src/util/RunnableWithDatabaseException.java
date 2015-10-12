package util;

import database.DatabaseException;

@FunctionalInterface
public interface RunnableWithDatabaseException {
	public void run() throws DatabaseException;
}
