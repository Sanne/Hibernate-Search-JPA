/*
 * Copyright 2015 Martin Braun
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.hotware.hsearch.db.events;

import com.github.hotware.hsearch.db.events.EventModelInfo.IdInfo;

/**
 * @author Martin
 *
 */
public class MySQLTriggerSQLStringSource implements TriggerSQLStringSource {

	public static final String DEFAULT_UNIQUE_ID_TABLE_NAME = "`_____unique____id____hsearch`";
	public static final String DEFAULT_UNIQUE_ID_PROCEDURE_NAME = "get_unique_id_hsearch";

	private static final String CREATE_TRIGGER_ORIGINAL_TABLE_SQL_FORMAT = ""
			+ "CREATE TRIGGER %s AFTER %s ON %s                 \n"
			+ "FOR EACH ROW                                     \n"
			+ "BEGIN                                            \n"
			+ "    CALL %s(@unique_id);                         \n"
			+ "    INSERT INTO %s(id, %s, %s) \n"
			+ "		VALUES(@unique_id, %s, %s);                 \n"
			+ "END;                                             \n";
	private static final String CREATE_TRIGGER_CLEANUP_SQL_FORMAT = ""
			+ "CREATE TRIGGER %s AFTER DELETE ON %s                  \n"
			+ "FOR EACH ROW                                          \n"
			+ "BEGIN                                                 \n"
			+ "DELETE FROM #UNIQUE_ID_TABLE_NAME# WHERE id = OLD.id; \n"
			+ "END;                                                  \n";
	private static final String DROP_TRIGGER_SQL_FORMAT = ""
			+ "DROP TRIGGER IF EXISTS %s;\n";

	private final String uniqueIdTableName;
	private final String uniqueIdProcedureName;

	// we don't support dropping the unique_id_table_name
	// because otherwise we would lose information about the last used
	// ids
	private String createTriggerCleanUpSQLFormat;
	private String createUniqueIdTable;
	private String dropUniqueIdTable;
	private String dropUniqueIdProcedure;
	private String createUniqueIdProcedure;

	public MySQLTriggerSQLStringSource() {
		this(DEFAULT_UNIQUE_ID_TABLE_NAME, DEFAULT_UNIQUE_ID_PROCEDURE_NAME);
	}

	public MySQLTriggerSQLStringSource(String uniqueIdTableName,
			String uniqueIdProcedureName) {
		this.uniqueIdTableName = uniqueIdTableName;
		this.uniqueIdProcedureName = uniqueIdProcedureName;
		this.init();
	}

	private void init() {
		this.createUniqueIdTable = String.format(
				"CREATE TABLE IF NOT EXISTS %s (                  \n"
						+ "id BIGINT(64) NOT NULL AUTO_INCREMENT, \n"
						+ " PRIMARY KEY (id)                      \n"
						+ ");                                     \n",
				this.uniqueIdTableName);
		this.dropUniqueIdTable = String.format("DROP TABLE IF EXISTS %s;",
				this.uniqueIdTableName);
		this.dropUniqueIdProcedure = String.format(
				"DROP PROCEDURE IF EXISTS %s;                 \n",
				this.uniqueIdProcedureName);
		this.createUniqueIdProcedure = String.format(
				"CREATE PROCEDURE %s                          \n"
						+ "(OUT ret BIGINT)                   \n"
						+ "BEGIN                              \n"
						+ "	INSERT INTO %s VALUES ();         \n"
						+ "	SET ret = last_insert_id();       \n"
						+ "END;                               \n",
				this.uniqueIdProcedureName, this.uniqueIdTableName);
		this.createTriggerCleanUpSQLFormat = CREATE_TRIGGER_CLEANUP_SQL_FORMAT
				.replaceAll("#UNIQUE_ID_TABLE_NAME#", this.uniqueIdTableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.hotware.hsearch.db.events.TriggerSQLStringSource#getSetupCode
	 * ()
	 */
	@Override
	public String[] getSetupCode() {
		return new String[] { createUniqueIdTable, dropUniqueIdProcedure,
				createUniqueIdProcedure };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.hotware.hsearch.db.events.TriggerSQLStringSource#getTriggerString
	 * (com.github.hotware.hsearch.db.events.EventModelInfo, int)
	 */
	@Override
	public String[] getTriggerCreationCode(EventModelInfo eventModelInfo,
			int eventType) {
		String originalTableName = eventModelInfo.getOriginalTableName();
		String triggerName = this.getTriggerName(
				eventModelInfo.getOriginalTableName(), eventType);
		String tableName = eventModelInfo.getTableName();
		String eventTypeColumn = eventModelInfo.getEventTypeColumn();
		StringBuilder valuesFromOriginal = new StringBuilder();
		StringBuilder idColumnNames = new StringBuilder();
		int addedVals = 0;
		for (IdInfo idInfo : eventModelInfo.getIdInfos()) {
			for (int i = 0; i < idInfo.getColumns().length; ++i) {
				if (addedVals > 0) {
					valuesFromOriginal.append(", ");
					idColumnNames.append(", ");
				}
				if (eventType == EventType.DELETE) {
					valuesFromOriginal.append("OLD.");
				} else {
					valuesFromOriginal.append("NEW.");
				}
				valuesFromOriginal.append(idInfo.getColumnsInOriginal()[i]);
				idColumnNames.append(idInfo.getColumns()[i]);
				++addedVals;
			}
		}
		if (addedVals == 0) {
			throw new IllegalArgumentException(
					"eventModelInfo didn't contain any idInfos");
		}
		String eventTypeValue = String.valueOf(eventType);
		String createTriggerOriginalTableSQL = new StringBuilder().append(
				String.format(CREATE_TRIGGER_ORIGINAL_TABLE_SQL_FORMAT,
						triggerName, EventType.toString(eventType),
						originalTableName, uniqueIdProcedureName, tableName,
						eventTypeColumn, idColumnNames.toString(),
						eventTypeValue, valuesFromOriginal.toString()))
				.toString();
		return new String[] { createTriggerOriginalTableSQL };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.hotware.hsearch.db.events.TriggerSQLStringSource#
	 * getTriggerDeletionString
	 * (com.github.hotware.hsearch.db.events.EventModelInfo, int)
	 */
	@Override
	public String[] getTriggerDropCode(EventModelInfo eventModelInfo,
			int eventType) {
		String triggerName = this.getTriggerName(
				eventModelInfo.getOriginalTableName(), eventType);
		return new String[] { String.format(DROP_TRIGGER_SQL_FORMAT,
				triggerName).toUpperCase() };
	}

	private String getTriggerName(String originalTableName, int eventType) {
		return new StringBuilder().append(originalTableName)
				.append("_updates_hsearch_")
				.append(EventType.toString(eventType)).toString();
	}

	private String getCleanUpTriggerName(String updatesTableName) {
		return new StringBuilder().append(updatesTableName)
				.append("_cleanup_hsearch").toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.hotware.hsearch.db.events.TriggerSQLStringSource#
	 * getSpecificSetupCode(com.github.hotware.hsearch.db.events.EventModelInfo)
	 */
	@Override
	public String[] getSpecificSetupCode(EventModelInfo eventModelInfo) {
		String createTriggerCleanUpSQL = String.format(
				this.createTriggerCleanUpSQLFormat,
				this.getCleanUpTriggerName(eventModelInfo.getTableName()),
				eventModelInfo.getTableName());
		return new String[] { createTriggerCleanUpSQL };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.hotware.hsearch.db.events.TriggerSQLStringSource#
	 * getSpecificUnSetupCode
	 * (com.github.hotware.hsearch.db.events.EventModelInfo)
	 */
	@Override
	public String[] getSpecificUnSetupCode(EventModelInfo eventModelInfo) {
		return new String[] { String.format(DROP_TRIGGER_SQL_FORMAT,
				this.getCleanUpTriggerName(eventModelInfo.getTableName())) };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.hotware.hsearch.db.events.TriggerSQLStringSource#
	 * getRecreateUniqueIdTableCode()
	 */
	@Override
	public String[] getRecreateUniqueIdTableCode() {
		return new String[] { this.dropUniqueIdTable, this.createUniqueIdTable };
	}
}
