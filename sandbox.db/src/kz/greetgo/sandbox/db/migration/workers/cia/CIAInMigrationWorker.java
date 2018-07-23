package kz.greetgo.sandbox.db.migration.workers.cia;

import kz.greetgo.sandbox.db.migration.workers.SqlWorker;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CIAInMigrationWorker extends SqlWorker implements AutoCloseable {

  public PreparedStatement clientsStatement;
  public PreparedStatement phoneStatement;
  public PreparedStatement addressStatement;

  public CIAInMigrationWorker(Connection connection) throws SQLException {
    super(connection);
    clientsStatement = connection.prepareStatement("insert into temp_client(client_id, name, surname, patronymic, gender, charm, birth_date,created_at)" +
      "values(?,?,?,?,?,?,?,?) on conflict (client_id)\n" +
      "  do update\n" +
      "    set\n" +
      "      surname    = case when EXCLUDED.surname notnull\n" +
      "        then EXCLUDED.surname end,\n" +
      "      name       = case when EXCLUDED.name notnull\n" +
      "        then EXCLUDED.name end,\n" +
      "      patronymic = EXCLUDED.patronymic,\n" +
      "      birth_date = case when EXCLUDED.birth_date notnull\n" +
      "        then EXCLUDED.birth_date end,\n" +
      "      gender     = case when EXCLUDED.gender notnull\n" +
      "        then EXCLUDED.gender end,\n" +
      "      charm      = case when EXCLUDED.charm notnull\n" +
      "        then EXCLUDED.charm end;");
    phoneStatement = connection.prepareStatement("insert into temp_phone (client_id, number, type) values(?,?,?)");

    addressStatement = connection.prepareStatement("insert into temp_address(client_id,street, house, flat, type) values(?,?,?,?,?) on conflict (client_id, type)\n" +
      "  do update\n" +
      "    set street = case when EXCLUDED.street notnull and EXCLUDED.street != ''\n" +
      "      then EXCLUDED.street end,\n" +
      "      flat = case when EXCLUDED.flat notnull and EXCLUDED.flat != ''\n" +
      "        then EXCLUDED.flat end,\n" +
      "      house = case when EXCLUDED.house notnull and EXCLUDED.house != ''\n" +
      "        then EXCLUDED.house end");

  }

  public void createTempTables() throws SQLException {
    exec("create table if not exists temp_client ( \n" +
      "          created_at timestamp not null ,\n" +
      "          client_id varchar(40) primary key,  \n" +
      "          name varchar(30), \n" +
      "          surname varchar(30), \n" +
      "          patronymic varchar(30), \n" +
      "          gender varchar(10), \n" +
      "          birth_date date, \n" +
      "          charm varchar(15), \n" +
      "          error text);");

    exec("create table if not exists temp_phone(" +
      "client_id varchar(40)," +
      "number varchar(30)," +
      "type varchar(10))," +
      "error varchar(50);");

    exec("create table if not exists temp_address (\n" +
      "  client_id varchar(40),\n" +
      "  street    varchar(100),\n" +
      "  flat      varchar(100),\n" +
      "  house     varchar(100),\n" +
      "  type      varchar(10),\n" +
      "  PRIMARY KEY (client_id, type)\n" +
      ");");

    connection.commit();
  }

  public void insertIntoClient() throws SQLException {
    exec("alter table client\n" +
      "  add column if not exists migr_client_id varchar(40);\n");

    connection.commit();

    exec("insert into characters (name) select charm\n" +
      "                              from temp_client\n" +
      "                              where charm notnull\n" +
      "                              group by charm;");

    connection.commit();

    exec("insert into\n" +
      "  client (name, surname, patronymic, gender, birth_date, charm, migr_client_id)\n" +
      "  select\n" +
      "    temp_client.name,\n" +
      "    temp_client.surname,\n" +
      "    temp_client.patronymic,\n" +
      "    temp_client.gender,\n" +
      "    temp_client.birth_date,\n" +
      "    characters.id,\n" +
      "    temp_client.client_id\n" +
      "  from temp_client, characters\n" +
      "  where temp_client.charm = characters.name and error isnull");


    connection.commit();

  }

  public void insertIntoPhone() throws SQLException {
    exec("insert into client_phone (client_id, number, type)\n" +
      "  select\n" +
      "    client.id,\n" +
      "    temp_phone.number,\n" +
      "    temp_phone.type\n" +
      "  from client, temp_phone\n" +
      "  where client.migr_client_id = temp_phone.client_id and error isnull;");
    connection.commit();
  }

  public void insertIntoAddress() throws SQLException {
    exec("insert into client_address (client_id, type, street, house, flat)\n" +
      "  SELECT\n" +
      "    client.id,\n" +
      "    temp_address.type,\n" +
      "    temp_address.street,\n" +
      "    temp_address.house,\n" +
      "    temp_address.flat\n" +
      "  from client, temp_address\n" +
      "  where client.migr_client_id = temp_address.client_id and error isnull;");
    connection.commit();
  }


  public void dropTempTables() throws SQLException {

    exec("alter table client drop column migr_client_id;");

    exec("drop table temp_client");
    exec("drop table temp_phone");
    exec("drop table temp_address");

    connection.commit();

  }

  public void updateError() throws SQLException, IOException {
    exec("update temp_client\n" +
      "set error = case\n" +
      "            when name isnull or TRIM(name) = ''\n" +
      "              then 'Invalid name;'\n" +
      "            when surname isnull or TRIM(surname) = ''\n" +
      "              then 'Invalid surname;'\n" +
      "            when birth_date isnull\n" +
      "              then 'Invalid birth date;'\n" +
      "            when charm isnull or TRIM(charm) = ''\n" +
      "              then 'Invalid charm;'\n" +
      "            when gender isnull or TRIM(gender) = ''\n" +
      "              then 'Invalid gender;'\n" +
      "            end;\n");
    exec("update temp_phone set error='No number' where number isnull;");

    exec("update temp_address set error='No street' where street isnull;");
    exec("update temp_address set error='No house' where house isnull;");
    exec("update temp_address set error='No flat' where flat isnull;");
    connection.commit();
    new File("build").mkdirs();

    CopyManager copyManager = new CopyManager((BaseConnection) connection);
    copyManager.copyOut("COPY (select * from temp_client where error notnull) to STDOUT ", new FileOutputStream(new File("build/error_client.csv")));
    copyManager.copyOut("COPY (select * from temp_address where error notnull) to STDOUT ", new FileOutputStream(new File("build/error_address.csv")));
    copyManager.copyOut("COPY (select * from temp_phone where error notnull) to STDOUT ", new FileOutputStream(new File("build/error_phone.csv")));

    connection.commit();
  }

  @Override
  public void close() throws Exception {

    addressStatement.close();
    clientsStatement.close();
    phoneStatement.close();

  }
}
