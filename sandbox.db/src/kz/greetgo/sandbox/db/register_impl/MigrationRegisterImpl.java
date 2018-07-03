package kz.greetgo.sandbox.db.register_impl;

import kz.greetgo.sandbox.controller.register.MigrationRegister;
import kz.greetgo.sandbox.db.migration.*;
import kz.greetgo.sandbox.db.migration.reader.XMLManager;


public class MigrationRegisterImpl implements MigrationRegister {

  @Override
  public void migrate() throws Exception {
    XMLManager xmlManager = new XMLManager("C:\\Programs\\Web\\Greetgo\\from_300.xml");

    InMigration inMigration = new InMigration();

    inMigration.execute();
    xmlManager.load(client -> inMigration.sendClient(client),
      phonesFromMigration -> {
      inMigration.sendPhones(phonesFromMigration);
    });
    //while (xmlManager.)
    System.out.println("FINISHED ALL TASKS------------------------");
  }

  public static void main(String[] args) throws Exception {
    MigrationRegisterImpl migrationRegister = new MigrationRegisterImpl();
    long startTime = System.nanoTime();
    migrationRegister.migrate();
    long endTime = System.nanoTime();
    long totalTime = endTime - startTime;
    Thread.sleep(1000);
    System.out.println(totalTime / 1000000000.0);
  }
}