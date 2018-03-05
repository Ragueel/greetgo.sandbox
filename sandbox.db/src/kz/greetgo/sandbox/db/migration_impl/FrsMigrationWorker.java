package kz.greetgo.sandbox.db.migration_impl;

import kz.greetgo.depinject.core.Bean;
import kz.greetgo.depinject.core.BeanGetter;
import kz.greetgo.sandbox.controller.model.ClientRecordsToSave;
import kz.greetgo.sandbox.db.configs.DbConfig;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static kz.greetgo.sandbox.db.util.TimeUtils.recordsPerSecond;
import static kz.greetgo.sandbox.db.util.TimeUtils.showTime;

@Bean
public class FrsMigrationWorker extends AbstractMigrationWorker {
  public BeanGetter<DbConfig> dbConfig;

  @Override
  protected void dropTmpTables() throws SQLException {
    exec("DROP TABLE IF EXISTS TMP_ACCOUNT, TMP_TRANSACTION");
  }

  @Override
  protected void handleErrors() {

  }

  @Override
  protected void uploadAndDropErrors() {

  }

  @Override
  protected void createTmpTables() throws SQLException {
    //language=PostgreSQL
    exec("CREATE TABLE TMP_ACCOUNT (\n" +
      "  type           VARCHAR(32),\n" +
      "  client_id      VARCHAR(32),\n" +
      "  account_number VARCHAR(64),\n" +
      "  registered_at  TIMESTAMP WITH TIME ZONE,\n" +
      "\n" +
      "  status         INT NOT NULL DEFAULT 0,\n" +
      "  error          VARCHAR(255),\n" +
      "  number         BIGSERIAL PRIMARY KEY\n" +
      ")");

    //language=PostgreSQL
    exec("CREATE TABLE TMP_TRANSACTION (\n" +
      "  type             VARCHAR(32),\n" +
      "  money            REAL,\n" +
      "  finished_at      TIMESTAMP WITH TIME ZONE,\n" +
      "  transaction_type VARCHAR(255),\n" +
      "  account_number   VARCHAR(100),\n" +
      "\n" +
      "  status           INT NOT NULL DEFAULT 0,\n" +
      "  error            VARCHAR(255),\n" +
      "  number           BIGSERIAL PRIMARY KEY\n" +
      ")");
  }

  @Override
  protected long migrateFromTmp() {
    return 0;
  }

  @Override
  protected int download() throws IOException, SQLException {
    List<String> fileDirToLoad = renameFiles(".json_row.txt.tar.bz2");

    for (String fileName : fileDirToLoad) {
      inputStream = new FileInputStream(fileName);
      TarArchiveInputStream tarInput = new TarArchiveInputStream(new BZip2CompressorInputStream(inputStream));
      TarArchiveEntry currentEntry = tarInput.getNextTarEntry();

      final AtomicBoolean working = new AtomicBoolean(true);
      final AtomicBoolean showStatus = new AtomicBoolean(false);

      final Thread see = new Thread(() -> {

        while (working.get()) {

          try {
            Thread.sleep(showStatusPingMillis);
          } catch (InterruptedException e) {
            break;
          }

          showStatus.set(true);

        }

      });
      see.start();

      int recordsCount;
      long startedAt = System.nanoTime();

      // parse xml and insert into tmp tables
      connection.setAutoCommit(false);

      try (TableWorker tableWorker = new TableWorker(connection, maxBatchSize)) {
        FrsParser frsParser = new FrsParser(tarInput, tableWorker);
        recordsCount = frsParser.parseAndSave();
      } finally {
        connection.setAutoCommit(true);
      }

      if (showStatus.get()) {
        showStatus.set(false);

        long now = System.nanoTime();
        info(" -- downloaded records " + recordsCount + " for " + showTime(now, startedAt)
          + " : " + recordsPerSecond(recordsCount, now - startedAt));
      }

      {
        long now = System.nanoTime();
        info("TOTAL Downloaded records " + recordsCount + " for " + showTime(now, startedAt)
          + " : " + recordsPerSecond(recordsCount, now - startedAt));
      }
    }

    return 0;
  }

  @Override
  public int migrate() throws Exception {
    createPostgresConnection();
    dropTmpTables();
    createTmpTables();
    int recordsSize = download();
    return 0;
  }

  private void createPostgresConnection() throws Exception {
    connection = DriverManager.getConnection(
      dbConfig.get().url(),
      dbConfig.get().username(),
      dbConfig.get().password()
    );
  }
}
