package kz.greetgo.sandbox.db.migration;

import kz.greetgo.depinject.core.BeanGetter;
import kz.greetgo.sandbox.db.configs.DbConfig;
import kz.greetgo.sandbox.db.migration.reader.json.JSONManager;
import kz.greetgo.sandbox.db.migration.reader.objects.TempAccount;
import kz.greetgo.sandbox.db.migration.reader.objects.TempTransaction;
import kz.greetgo.sandbox.db.migration.workers.frs.FRSInMigrationWorker;
import kz.greetgo.sandbox.db.stand.model.ClientAccountDot;
import kz.greetgo.sandbox.db.stand.model.ClientTransactionDot;
import kz.greetgo.sandbox.db.test.dao.FRSMigrationTestDao;
import kz.greetgo.sandbox.db.test.util.ParentTestNg;
import kz.greetgo.util.RND;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.api.Assertions.assertThat;

public class FRSInMigrationTest extends ParentTestNg {

  public BeanGetter<FRSMigrationTestDao> frsDao;
  public BeanGetter<DbConfig> dbConfig;

  @BeforeMethod
  public void dropTables() throws Exception {
    frsDao.get().createMigrClientIdColumn();
    frsDao.get().createTempClientTable();
    frsDao.get().deleteClients();
    frsDao.get().createTempAccountTable();
    frsDao.get().createTempTransactionTable();

  }

  @AfterMethod
  public void createTables() throws Exception {
    frsDao.get().dropAccountTable();
    frsDao.get().dropTransactionTable();
  }

  @Test
  public void testInsertTransactionIntoTemp() throws IOException, ParseException, SQLException {
    try (Connection connection = connectToDatabase()) {
      FRSInMigrationWorker frsInMigration = new FRSInMigrationWorker(connection);

      List<TempTransaction> transactions = createTransactions();

      JSONManager jsonManager = new JSONManager("build/test_frs.txt");
      jsonManager.load(connection, frsInMigration.accountsStatement, frsInMigration.transactionStatement);

      List<kz.greetgo.sandbox.db.classes.TempTransaction> transactionsFromMigration = frsDao.get().getTempTransactions();

      assertThat(transactionsFromMigration).hasSameSizeAs(transactions);

      for (int i = 0; i < transactionsFromMigration.size(); i++) {
        assertThat(transactionsFromMigration.get(i).account_number).isEqualTo(transactions.get(i).account_number);
        assertThat(transactionsFromMigration.get(i).finished_at.toString()).isEqualTo(transactions.get(i).finished_at);
      }
    }

  }

  @Test
  public void testAccountErrorNoNumber() throws IOException, SQLException, ParseException {
    try (Connection connection = connectToDatabase()) {
      FRSInMigrationWorker frsInMigration = new FRSInMigrationWorker(connection);

      TempAccount account = createAccountWithNullNumber();
      JSONManager jsonManager = new JSONManager("build/test_frs.txt");
      jsonManager.load(connection, frsInMigration.accountsStatement, frsInMigration.transactionStatement);

      frsDao.get().insertNewClient(frsDao.get().insertNewCharm());

      frsInMigration.updateError();


      List<kz.greetgo.sandbox.db.classes.TempAccount> tempAccounts = frsDao.get().getTempAccounts();
      assertThat(tempAccounts).hasSize(1);

      assertThat(tempAccounts.get(0).error).isEqualTo("No account number;");
    }

  }

  @Test
  public void testInsertAccountIntoTemp() throws IOException, SQLException, ParseException {
    try (Connection connection = connectToDatabase()) {
      FRSInMigrationWorker frsInMigration = new FRSInMigrationWorker(connection);

      List<TempAccount> accounts = createAccounts();

      JSONManager jsonManager = new JSONManager("build/test_frs.txt");
      jsonManager.load(connection, frsInMigration.accountsStatement, frsInMigration.transactionStatement);


      List<kz.greetgo.sandbox.db.classes.TempAccount> tempAccounts = frsDao.get().getTempAccounts();

      assertThat(tempAccounts).hasSameSizeAs(accounts);

      for (int i = 0; i < tempAccounts.size(); i++) {
        assertThat(tempAccounts.get(i).client_id).isEqualTo(accounts.get(i).client_id);
        assertThat(tempAccounts.get(i).registered_at.toString()).isEqualTo(accounts.get(i).registered_at);
        assertThat(tempAccounts.get(i).account_number).isEqualTo(accounts.get(i).account_number);
      }
    }
  }

  @Test
  public void testInsertAccountIntoReal() throws IOException, SQLException, ParseException {
    try (Connection connection = connectToDatabase()) {
      FRSInMigrationWorker frsInMigration = new FRSInMigrationWorker(connection);

      List<TempAccount> accounts = createAccounts();

      JSONManager jsonManager = new JSONManager("build/test_frs.txt");
      jsonManager.load(connection, frsInMigration.accountsStatement, frsInMigration.transactionStatement);


      frsDao.get().insertNewClient(frsDao.get().insertNewCharm());

      frsInMigration.updateError();
      frsInMigration.insertIntoAccount();

      List<ClientAccountDot> accountDots = frsDao.get().getAccountDots();

      assertThat(accountDots).hasSize(1);

      for (int i = 0; i < accountDots.size(); i++) {
        assertThat(accountDots.get(i).number).isEqualTo(accounts.get(i).account_number);
        assertThat(accountDots.get(i).registered_at.toString()).isEqualTo(accounts.get(i).registered_at);
      }
    }
  }

  @Test
  public void testInsertTransactionIntoReal() throws IOException, SQLException, ParseException {
    try (Connection connection = connectToDatabase()) {
      FRSInMigrationWorker frsInMigration = new FRSInMigrationWorker(connection);

      List<TempTransaction> transactions = createTransactions();

      JSONManager jsonManager = new JSONManager("build/test_frs.txt");
      jsonManager.load(connection, frsInMigration.accountsStatement, frsInMigration.transactionStatement);


      int clientId = frsDao.get().insertNewClient(frsDao.get().insertNewCharm());
      int accId = frsDao.get().insertClientAccount1(clientId);

      frsInMigration.updateError();
      frsInMigration.insertIntoTransaction();

      List<ClientTransactionDot> transactionDots = frsDao.get().getTransactionsFromReal(accId);

      assertThat(transactionDots).hasSize(1);

      for (int i = 0; i < transactionDots.size(); i++) {
        assertThat(transactionDots.get(i).money).isEqualTo(Double.valueOf(transactions.get(i).money));
        assertThat(transactionDots.get(i).finished_at.toString()).isEqualTo(transactions.get(i).finished_at);
      }
    }
  }

  @Test
  public void testCheckFullMigration() throws SQLException, IOException, ParseException {
    try (Connection connection = connectToDatabase()) {
      List<TempTransaction> transactions = createFullMigrationFile();

      FRSInMigrationWorker frsInMigration = new FRSInMigrationWorker(connection);

      JSONManager jsonManager = new JSONManager("build/test_frs.txt");
      jsonManager.load(connection, frsInMigration.accountsStatement, frsInMigration.transactionStatement);

      frsDao.get().insertNewClient(frsDao.get().insertNewCharm());

      frsInMigration.updateError();
      frsInMigration.insertIntoAccount();
      frsInMigration.insertIntoTransaction();

      List<ClientAccountDot> accountDots = frsDao.get().getAccountDots();

      assertThat(accountDots).hasSize(1);

      for (int i = 0; i < accountDots.size(); i++) {
        assertThat(accountDots.get(i).number).isEqualTo("1");
      }

      List<ClientTransactionDot> transactionDots = frsDao.get().getTransactionsFromReal(accountDots.get(0).id);

      assertThat(transactionDots).hasSize(2);

      for (int i = 0;i<transactionDots.size();i++){
        assertThat(transactionDots.get(i).money).isEqualTo(Double.valueOf(transactions.get(i).money));
        assertThat(transactionDots.get(i).finished_at.toString()).isEqualTo(transactions.get(i).finished_at);
      }
    }
  }

  private List<TempTransaction> createFullMigrationFile() throws FileNotFoundException, UnsupportedEncodingException {
    List<TempTransaction> tempTransactions = new ArrayList<>();
    PrintWriter writer = new PrintWriter("build/test_frs.txt", "UTF-8");
    TempAccount account = new TempAccount();
    account.client_id = "1";
    account.account_number = "1";
    account.registered_at = new Timestamp(new Date().getTime()).toString();
    writer.println(account.toJson());

    TempTransaction transaction = new TempTransaction();
    transaction.finished_at = new Timestamp(new Date().getTime()).toString();
    transaction.money = "+100000";
    transaction.account_number = "1";
    transaction.transaction_type = RND.str(10);
    tempTransactions.add(transaction);

    writer.println(transaction.toJson());

    TempAccount account1 = new TempAccount();
    account1.client_id = "2";
    account1.account_number = null;
    account1.registered_at = new Timestamp(new Date().getTime()).toString();


    TempTransaction transaction1 = new TempTransaction();
    transaction1.finished_at = new Timestamp(new Date().getTime()).toString();
    transaction1.money = "+100000";
    transaction1.account_number = "1";
    transaction1.transaction_type = "null";
    writer.println(transaction1.toJson());

    TempTransaction transaction2 = new TempTransaction();
    transaction2.finished_at = new Timestamp(new Date().getTime()).toString();
    transaction2.money = "+100000";
    transaction2.account_number = "1";
    transaction2.transaction_type = RND.str(10);
    tempTransactions.add(transaction2);
    writer.println(transaction2.toJson());

    writer.close();

    return tempTransactions;

  }

  private TempAccount createAccountWithNullNumber() throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter("build/test_frs.txt", "UTF-8");

    TempAccount account = new TempAccount();
    account.client_id = "1";
    account.account_number = null;
    account.registered_at = new Timestamp(new Date().getTime()).toString();

    writer.println(account.toJson());
    writer.close();
    return account;
  }

  private List<TempAccount> createAccounts() throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter("build/test_frs.txt", "UTF-8");
    List<TempAccount> accounts = new ArrayList<>();

    TempAccount account = new TempAccount();
    account.client_id = "1";
    account.registered_at = new Timestamp(new Date().getTime()).toString();
    account.account_number = "1";
    writer.println(account.toJson());
    accounts.add(account);

    TempAccount account1 = new TempAccount();
    account1.client_id = "2";
    account1.registered_at = new Timestamp(new Date().getTime()).toString();
    account1.account_number = "2";
    writer.println(account1.toJson());
    accounts.add(account1);
    writer.close();
    return accounts;
  }

  private List<TempTransaction> createTransactions() throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter("build/test_frs.txt", "UTF-8");
    List<TempTransaction> transactions = new ArrayList<>();
    TempTransaction transaction = new TempTransaction();
    transaction.finished_at = new Timestamp(new Date().getTime()).toString();
    transaction.money = "+100000";
    transaction.account_number = "1";
    transaction.transaction_type = RND.str(10);

    transactions.add(transaction);
    writer.println(transaction.toJson());

    TempTransaction transaction1 = new TempTransaction();
    transaction1.finished_at = new Timestamp(new Date().getTime()).toString();
    transaction1.money = "+100000";
    transaction1.account_number = "2";
    transaction1.transaction_type = RND.str(10);

    transactions.add(transaction1);
    writer.println(transaction1.toJson());
    writer.close();
    return transactions;
  }

  private TempTransaction getRandomTransaction() {
    TempTransaction transaction = new TempTransaction();
    transaction.money = RND.plusDouble(30, 10) + "";
    transaction.account_number = RND.str(20);
    transaction.finished_at = new Timestamp(new Date().getTime()) + "";
    transaction.type = RND.str(10);
    transaction.transaction_type = RND.str(10);
    return transaction;
  }

  @SuppressWarnings("Duplicates")
  private Connection connectToDatabase() throws SQLException {
    String url = dbConfig.get().url();
    Properties properties = new Properties();
    properties.setProperty("user", dbConfig.get().username());
    properties.setProperty("password", dbConfig.get().password());
    Connection connection = DriverManager.getConnection(url, properties);
    connection.setAutoCommit(false);
    return connection;
  }

}
