/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.crossdata.catalog

import java.sql.{Connection, DriverManager, ResultSet}

import com.stratio.common.utils.components.logger.impl.SparkLoggerComponent
import org.apache.spark.sql.catalyst.{CatalystConf, SimpleCatalystConf, TableIdentifier}
import org.apache.spark.sql.crossdata._
import org.apache.spark.sql.types.StructType

import scala.annotation.tailrec

object PostgreSQLCatalog {
  // SQLConfig
  val Driver = "jdbc.driver"
  val Url = "jdbc.url"
  val Database = "jdbc.db.name"
  val Table = "jdbc.db.table"
  val TableWithViewMetadata = "jdbc.db.view"
  val User = "jdbc.db.user"
  val Pass = "jdbc.db.pass"
  // CatalogFields
  val DatabaseField = "db"
  val TableNameField = "tableName"
  val SchemaField = "tableSchema"
  val DatasourceField = "datasource"
  val PartitionColumnField = "partitionColumn"
  val OptionsField = "options"
  val CrossdataVersionField = "crossdataVersion"
  val SqlViewField = "sqlView"

}

/**
  * Default implementation of the [[org.apache.spark.sql.crossdata.catalog.XDCatalog]] with persistence using
  * Jdbc.
  * Supported MySQL and PostgreSQL
  * @param conf An implementation of the [[CatalystConf]].
  */
class PostgreSQLCatalog(override val conf: CatalystConf = new SimpleCatalystConf(true), xdContext: XDContext)
  extends XDCatalog(conf, xdContext) with SparkLoggerComponent {

  import PostgreSQLCatalog._
  import XDCatalog._

  private val config = XDContext.catalogConfig

  private val db = config.getString(Database)
  private val table = config.getString(Table)
  private val tableWithViewMetadata = config.getString(TableWithViewMetadata)

  @transient lazy val connection: Connection = {

    val driver = config.getString(Driver)
    val user = config.getString(User)
    val pass = config.getString(Pass)
    val url = config.getString(Url)

    Class.forName(driver)
    try{
      val jdbcConnection = DriverManager.getConnection(url, user, pass)

      // CREATE PERSISTENT METADATA TABLE
      if(!schemaExists(db, jdbcConnection))
        jdbcConnection.createStatement().executeUpdate(s"CREATE SCHEMA $db")

      jdbcConnection.createStatement().executeUpdate(
        s"""|CREATE TABLE IF NOT EXISTS $db.$table (
            |$DatabaseField VARCHAR(50),
            |$TableNameField VARCHAR(50),
            |$SchemaField TEXT,
            |$DatasourceField TEXT,
            |$PartitionColumnField TEXT,
            |$OptionsField TEXT,
            |$CrossdataVersionField TEXT,
            |PRIMARY KEY ($DatabaseField,$TableNameField))""".stripMargin)

      jdbcConnection.createStatement().executeUpdate(
        s"""|CREATE TABLE IF NOT EXISTS $db.$tableWithViewMetadata (
            |$DatabaseField VARCHAR(50),
            |$TableNameField VARCHAR(50),
            |$SqlViewField TEXT,
            |$CrossdataVersionField VARCHAR(30),
            |PRIMARY KEY ($DatabaseField,$TableNameField))""".stripMargin)

      jdbcConnection
    }catch{
      case e:Exception =>
        logError(e.getMessage)
        null
    }

  }


  override def lookupTable(tableIdentifier: TableIdentifier): Option[CrossdataTable] = {

    val preparedStatement = connection.prepareStatement(s"SELECT * FROM $db.$table WHERE $DatabaseField= ? AND $TableNameField= ?")
    preparedStatement.setString(1, tableIdentifier.database.getOrElse(""))
    preparedStatement.setString(2, tableIdentifier.table)
    val resultSet = preparedStatement.executeQuery()

    if (!resultSet.isBeforeFirst) {
      None
    } else {
      resultSet.next()
      val database = resultSet.getString(DatabaseField)
      val table = resultSet.getString(TableNameField)
      val schemaJSON = resultSet.getString(SchemaField)
      val partitionColumn = resultSet.getString(PartitionColumnField)
      val datasource = resultSet.getString(DatasourceField)
      val optsJSON = resultSet.getString(OptionsField)
      val version = resultSet.getString(CrossdataVersionField)

      Some(
        CrossdataTable(table, Some(database), getUserSpecifiedSchema(schemaJSON), datasource, getPartitionColumn(partitionColumn), getOptions(optsJSON), version)
      )
    }
  }

  override def listPersistedTables(databaseName: Option[String]): Seq[(String, Boolean)] = {
    @tailrec
    def getSequenceAux(resultset: ResultSet, next: Boolean, set: Set[String] = Set()): Set[String] = {
      if (next) {
        val database = resultset.getString(DatabaseField)
        val table = resultset.getString(TableNameField)
        val tableId = if (database.trim.isEmpty) table else s"$database.$table"
        getSequenceAux(resultset, resultset.next(), set + tableId)
      } else {
        set
      }
    }

    val statement = connection.createStatement
    val dbFilter = databaseName.fold("")(dbName => s"WHERE $DatabaseField ='$dbName'")
    val resultSet = statement.executeQuery(s"SELECT $DatabaseField, $TableNameField FROM $db.$table $dbFilter")

    getSequenceAux(resultSet, resultSet.next).map(tableId => (tableId, false)).toSeq
  }

  override def persistTableMetadata(crossdataTable: CrossdataTable): Unit = {

    val tableSchema = serializeSchema(crossdataTable.schema.getOrElse(new StructType()))
    val tableOptions = serializeOptions(crossdataTable.opts)
    val partitionColumn = serializePartitionColumn(crossdataTable.partitionColumn)

    connection.setAutoCommit(false)

    // check if the database-table exist in the persisted catalog
    val preparedStatement = connection.prepareStatement(s"SELECT * FROM $db.$table WHERE $DatabaseField= ? AND $TableNameField= ?")
    preparedStatement.setString(1, crossdataTable.dbName.getOrElse(""))
    preparedStatement.setString(2, crossdataTable.tableName)
    val resultSet = preparedStatement.executeQuery()

    if (!resultSet.isBeforeFirst) {
      val prepped = connection.prepareStatement(
        s"""|INSERT INTO $db.$table (
            | $DatabaseField, $TableNameField, $SchemaField, $DatasourceField, $PartitionColumnField, $OptionsField, $CrossdataVersionField
            |) VALUES (?,?,?,?,?,?,?)
       """.stripMargin)
      prepped.setString(1, crossdataTable.dbName.getOrElse(""))
      prepped.setString(2, crossdataTable.tableName)
      prepped.setString(3, tableSchema)
      prepped.setString(4, crossdataTable.datasource)
      prepped.setString(5, partitionColumn)
      prepped.setString(6, tableOptions)
      prepped.setString(7, CrossdataVersion)
      prepped.execute()
    }
    else {
      val prepped = connection.prepareStatement(
        s"""|UPDATE $db.$table SET $SchemaField=?, $DatasourceField=?,$PartitionColumnField=?,$OptionsField=?,$CrossdataVersionField=?
            |WHERE $DatabaseField='${crossdataTable.dbName.getOrElse("")}' AND $TableNameField='${crossdataTable.tableName}';
       """.stripMargin.replaceAll("\n", " "))

      prepped.setString(1, tableSchema)
      prepped.setString(2, crossdataTable.datasource)
      prepped.setString(3, partitionColumn)
      prepped.setString(4, tableOptions)
      prepped.setString(5, CrossdataVersion)
      prepped.execute()
    }
    connection.commit()
    connection.setAutoCommit(true)
  }

  override def dropPersistedTable(tableIdentifier: TableIdentifier): Unit = {
    connection.createStatement.executeUpdate(s"DELETE FROM $db.$table WHERE tableName='${tableIdentifier.table}' AND db='${tableIdentifier.database.getOrElse("")}'")
  }

  override def dropAllPersistedTables(): Unit =
    connection.createStatement.executeUpdate(s"TRUNCATE $db.$table")

  def schemaExists(schema: String, connection: Connection): Boolean = {
    val statement = connection.createStatement()
    val result = statement.executeQuery(s"SELECT schema_name FROM information_schema.schemata WHERE schema_name = '$schema';")
    result.isBeforeFirst
  }

  override protected def lookupView(viewIdentifier: ViewIdentifier): Option[String] = {
    val resultSet = selectMetadata(tableWithViewMetadata, viewIdentifier)
    if (!resultSet.isBeforeFirst) {
      None
    } else {
      resultSet.next()
      Option(resultSet.getString(SqlViewField))
    }
  }

  override protected[crossdata] def persistViewMetadata(tableIdentifier: TableIdentifier, sqlText: String): Unit = {
    try {
      connection.setAutoCommit(false)
      val resultSet = selectMetadata(tableWithViewMetadata, tableIdentifier)

      if (!resultSet.isBeforeFirst) {
        val prepped = connection.prepareStatement(
          s"""|INSERT INTO $db.$tableWithViewMetadata (
              | $DatabaseField, $TableNameField, $SqlViewField, $CrossdataVersionField
              |) VALUES (?,?,?,?)
       """.stripMargin)
        prepped.setString(1, tableIdentifier.database.getOrElse(""))
        prepped.setString(2, tableIdentifier.table)
        prepped.setString(3, sqlText)
        prepped.setString(4, CrossdataVersion)
        prepped.execute()
      } else {
        val prepped =
          connection.prepareStatement(
            s"""|UPDATE $db.$tableWithViewMetadata SET $SqlViewField=?
                |WHERE $DatabaseField='${tableIdentifier.database.getOrElse("")}' AND $TableNameField='${tableIdentifier.table}'
             """.stripMargin.replaceAll("\n", " "))

        prepped.setString(1, sqlText)
        prepped.execute()
      }
      connection.commit()

    } finally {
      connection.setAutoCommit(true)
    }
  }

  private def selectMetadata(targetTable: String, tableIdentifier: TableIdentifier): ResultSet = {

    val preparedStatement = connection.prepareStatement(s"SELECT * FROM $db.$targetTable WHERE $DatabaseField= ? AND $TableNameField= ?")
    preparedStatement.setString(1, tableIdentifier.database.getOrElse(""))
    preparedStatement.setString(2, tableIdentifier.table)
    preparedStatement.executeQuery()

  }

  override protected def dropPersistedView(viewIdentifier: ViewIdentifier): Unit = {
    connection.createStatement.executeUpdate(
      s"DELETE FROM $db.$tableWithViewMetadata WHERE tableName='${viewIdentifier.table}' AND db='${viewIdentifier.database.getOrElse("")}'")
  }

  override protected def dropAllPersistedViews(): Unit = {
    connection.createStatement.executeUpdate(s"DELETE FROM $db.$tableWithViewMetadata")
  }

  override def checkConnectivity:Boolean = {
    connection!=null
  }
}