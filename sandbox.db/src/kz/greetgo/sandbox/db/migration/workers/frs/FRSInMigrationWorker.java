package kz.greetgo.sandbox.db.migration.workers.frs;

import kz.greetgo.sandbox.db.migration.workers.SqlWorker;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FRSInMigrationWorker extends SqlWorker implements AutoCloseable {

  public PreparedStatement accountsStatement;
  public PreparedStatement transactionStatement;

  public FRSInMigrationWorker(Connection connection) throws SQLException {
    super(connection);
    accountsStatement = connection.prepareStatement("insert into temp_account(account_number, client_id, registered_at) values(?,?,?)");
    transactionStatement = connection.prepareStatement("insert into temp_transaction(account_number, money, transaction_type, finished_at) values(?,?,?,?)");

  }

  public void createTempTables() throws SQLException {
    exec("create table if not exists temp_transaction(" +
      "finished_at timestamp," +
      "account_number varchar(100), " +
      "money float, " +
      "transaction_type varchar(200)," +
      "error varchar(100));");

    exec("create table if not exists temp_account (\n" +
      "  account_number varchar(100),\n" +
      "  client_id      varchar(40),\n" +
      "  registered_at  timestamp,\n" +
      "  error varchar(100)" +
      ");");

    connection.commit();
  }

  public void dropTempTables() throws SQLException {
    accountsStatement.close();
    transactionStatement.close();
  }

  public void updateError() throws SQLException, IOException {
    exec("update temp_account set error='No account number;' where account_number isnull or account_number='null'");

    exec("update temp_transaction\n" +
      "set error = 'No transaction type;'\n" +
      "where transaction_type = 'null';\n");

    CopyManager copyManager = new CopyManager((BaseConnection) connection);
    new File("build").mkdirs();

    copyManager.copyOut("COPY (select * from temp_transaction where error!='' and error notnull) to STDOUT ", new FileOutputStream(new File("build/error_frs_transaction.csv")));
    copyManager.copyOut("COPY (select * from temp_account where error!='' and error notnull) to STDOUT ", new FileOutputStream(new File("build/error_frs_account.csv")));

    connection.commit();
  }


  public void insertIntoAccount() throws SQLException {

    exec("insert into client_account (client_id, registered_at, number)\n" +
      "  select\n" +
      "    client.id,\n" +
      "    temp_account.registered_at,\n" +
      "    temp_account.account_number\n" +
      "  from temp_account, client\n" +
      "  where temp_account.client_id = client.migr_client_id and error isnull;");
    connection.commit();
  }

  public void insertIntoTransaction() throws SQLException {
    exec("insert into transaction_type (name)\n" +
      "  select temp_transaction.transaction_type\n" +
      "  from temp_transaction where transaction_type!='null'\n" +
      "  group by temp_transaction.transaction_type;");

    exec("insert into client_account_transaction (account, money, finished_at, type)\n" +
      "  select\n" +
      "    trans.id,\n" +
      "    trans.money,\n" +
      "    trans.finished_at,\n" +
      "    transaction_type.id\n" +
      "  from (select\n" +
      "          client_account.id,\n" +
      "          temp_transaction.money,\n" +
      "          temp_transaction.finished_at,\n" +
      "          temp_transaction.transaction_type\n" +
      "        from temp_transaction, client_account\n" +
      "        where temp_transaction.account_number = client_account.number) as trans, transaction_type\n" +
      "  where trans.transaction_type = transaction_type.name;");

    exec("update client_account\n" +
      "set money = (\n" +
      "  (select sum(client_account_transaction.money) as total\n" +
      "   from client_account as accs\n" +
      "     join client_account_transaction\n" +
      "       on client_account.client_id = client_account_transaction.account\n" +
      "   where accs.client_id = client_account.client_id\n" +
      "   group by client_account.client_id)\n" +
      ");");
    connection.commit();
  }


  @Override
  public void close() throws Exception {
    transactionStatement.close();
    accountsStatement.close();
  }
}
