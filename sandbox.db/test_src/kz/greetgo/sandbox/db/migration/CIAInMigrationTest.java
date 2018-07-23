package kz.greetgo.sandbox.db.migration;

import kz.greetgo.depinject.core.BeanGetter;
import kz.greetgo.sandbox.db.configs.DbConfig;
import kz.greetgo.sandbox.db.migration.reader.objects.TempAddress;
import kz.greetgo.sandbox.db.migration.reader.objects.TempClient;
import kz.greetgo.sandbox.db.migration.reader.objects.TempPhone;
import kz.greetgo.sandbox.db.migration.reader.xml.XMLManager;
import kz.greetgo.sandbox.db.migration.workers.cia.CIAInMigrationWorker;
import kz.greetgo.sandbox.db.stand.model.AddressDot;
import kz.greetgo.sandbox.db.stand.model.ClientDot;
import kz.greetgo.sandbox.db.stand.model.PhoneDot;
import kz.greetgo.sandbox.db.test.dao.CIAMigrationTestDao;
import kz.greetgo.sandbox.db.test.util.ParentTestNg;
import kz.greetgo.util.RND;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.api.Assertions.assertThat;

public class CIAInMigrationTest extends ParentTestNg {

  public BeanGetter<DbConfig> dbConfig;
  public BeanGetter<CIAMigrationTestDao> ciaMigrationDao;

  @BeforeMethod
  public void createTables() {

    ciaMigrationDao.get().createMigrClientIdColumn();

    dropAllTables();
    ciaMigrationDao.get().createTempClientTable();
    ciaMigrationDao.get().createTempAddressTable();
    ciaMigrationDao.get().createTempPhoneTable();
  }


  private void dropAllTables() {
    ciaMigrationDao.get().deleteFromCharms();
    ciaMigrationDao.get().deleteFromClient();
    ciaMigrationDao.get().dropTempClientTable();
    ciaMigrationDao.get().dropTempAddressTable();
    ciaMigrationDao.get().dropTempPhoneTable();
  }

  @Test
  public void testInsertClientIntoTemp() throws IOException, SAXException, ParserConfigurationException, SQLException {

    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      List<TempClient> clients = createClientXmlFile();

      //
      //
      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);
      //
      //

      List<TempClient> tempClients = ciaMigrationDao.get().getTempClients();

      assertThat(tempClients).hasSize(3);
      assertClients(clients, tempClients);
    }
  }

  @Test
  public void testInsertPhoneIntoTemp() throws IOException, SAXException, ParserConfigurationException, SQLException {
    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      List<TempPhone> phones = createPhones();

      //
      //
      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);
      //
      //


      List<TempPhone> tempPhones = ciaMigrationDao.get().getTempPhones();

      assertThat(tempPhones).hasSameSizeAs(phones);
      assertPhones(phones, tempPhones);
    }
  }

  @Test
  public void testClientErrorWithoutName() throws IOException, SAXException, ParserConfigurationException, SQLException {
    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      TempClient clientWithoutName = createClientWithoutName();

      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);


      inMigration.updateError();
      List<TempClient> clients = ciaMigrationDao.get().getTempClients();

      assertThat(clients).hasSize(1);

      assertThat(clients.get(0).error).isEqualTo("Invalid name;");
      assertThat(clients.get(0).client_id).isEqualTo(clientWithoutName.client_id);
      assertThat(clients.get(0).name).isEqualTo("");
    }
  }

  @Test
  public void testClientErrorWithoutSurname() throws IOException, SAXException, ParserConfigurationException, SQLException {
    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      TempClient clientWithoutName = createClientWithoutSurname();

      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);

      inMigration.updateError();
      List<TempClient> clients = ciaMigrationDao.get().getTempClients();

      assertThat(clients).hasSize(1);

      assertThat(clients.get(0).error).isEqualTo("Invalid surname;");
      assertThat(clients.get(0).client_id).isEqualTo(clientWithoutName.client_id);
      assertThat(clients.get(0).surname).isEqualTo("");
    }
  }

  @Test
  public void testClientErrorWithoutBirth() throws IOException, SAXException, ParserConfigurationException, SQLException {
    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      TempClient clientWithoutName = createClientWithoutBirth();

      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);

      inMigration.updateError();
      List<TempClient> clients = ciaMigrationDao.get().getTempClients();

      assertThat(clients).hasSize(1);

      assertThat(clients.get(0).error).isEqualTo("Invalid birth date;");
      assertThat(clients.get(0).client_id).isEqualTo(clientWithoutName.client_id);
      assertThat(clients.get(0).birth).isNull();
    }
  }

  @Test
  public void testInsertAddressIntoTemp() throws IOException, SAXException, ParserConfigurationException, SQLException {
    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      List<TempAddress> addresses = createAddressXml();

      //
      //
      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);
      //
      //

      List<TempAddress> tempAddress = ciaMigrationDao.get().getTempAddresses();

      assertThat(tempAddress).hasSameSizeAs(addresses);
      assertAddresses(addresses, tempAddress);
    }
  }


  @Test
  public void testInsertClientIntoReal() throws SQLException, IOException, ParserConfigurationException, SAXException {
    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      List<TempClient> clients = createClientXmlFile();

      //
      //
      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);
      //
      //

      inMigration.updateError();
      inMigration.insertIntoClient();
      List<ClientDot> clientDots = ciaMigrationDao.get().getClientDots();

      assertThat(clientDots).hasSize(2);
      for (int i = 0; i < clientDots.size(); i++) {
        assertThat(clientDots.get(i).name).isEqualTo(clients.get(i).name);
        assertThat(clientDots.get(i).surname).isEqualTo(clients.get(i).surname);
        assertThat(clientDots.get(i).patronymic).isEqualTo(clients.get(i).patronymic);
      }
    }
  }

  @Test
  public void testInsertPhoneIntoReal() throws IOException, SQLException, SAXException, ParserConfigurationException {
    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      insertClients();

      List<TempPhone> phones = createPhones();

      //
      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);
      //
      //
      inMigration.insertIntoPhone();


      List<PhoneDot> phoneDots = ciaMigrationDao.get().getPhonesFromReal();

      assertThat(phoneDots).hasSameSizeAs(phones);
      for (int i = 0; i < phoneDots.size(); i++) {
        assertThat(phoneDots.get(i).number).isEqualTo(phones.get(i).number);
        assertThat(phoneDots.get(i).type).isEqualTo(phones.get(i).type);
      }
    }

  }


  @Test
  public void testInsertAddressIntoReal() throws IOException, SQLException, SAXException, ParserConfigurationException {
    try (Connection connection = connectToDatabase()) {
      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);
      insertClients();

      List<TempAddress> addresses = createAddressXml();

      //
      //
      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);
      //
      //
      inMigration.insertIntoAddress();

      List<AddressDot> addressDots = ciaMigrationDao.get().getAddressDots();

      assertThat(addressDots).hasSize(4);

      for (int i = 0; i < addressDots.size(); i++) {
        assertThat(addressDots.get(i).flat).isEqualTo(addresses.get(i).flat);
        assertThat(addressDots.get(i).house).isEqualTo(addresses.get(i).house);
        assertThat(addressDots.get(i).street).isEqualTo(addresses.get(i).street);
        assertThat(addressDots.get(i).type).isEqualTo(addresses.get(i).type);
      }
    }
  }

  @Test
  public void testCheckFullMigration() throws SQLException, SAXException, IOException, ParserConfigurationException {
    try (Connection connection = connectToDatabase()) {
      createFullFile();
      List<TempClient> tempClients = getClientsFromFullFile();
      List<TempAddress> tempAddresses = getAddressesFromFullFile();
      List<TempPhone> tempPhones = getPhonesFromFullFile();

      CIAInMigrationWorker inMigration = new CIAInMigrationWorker(connection);

      //
      //
      XMLManager xmlManager = new XMLManager("build/test_cia.xml");
      xmlManager.load(connection, inMigration.clientsStatement, inMigration.phoneStatement, inMigration.addressStatement);
      //
      //

      inMigration.updateError();
      inMigration.insertIntoClient();
      inMigration.insertIntoAddress();
      inMigration.insertIntoPhone();

      List<ClientDot> clientDots = ciaMigrationDao.get().getClientDots();

      assertThat(clientDots).hasSameSizeAs(tempClients);
      for (int i = 0; i < clientDots.size(); i++) {
        assertThat(clientDots.get(i).name).isEqualTo(tempClients.get(i).name);
        assertThat(clientDots.get(i).surname).isEqualTo(tempClients.get(i).surname);
        assertThat(clientDots.get(i).patronymic).isEqualTo(tempClients.get(i).patronymic);
      }

      List<PhoneDot> phoneDots = ciaMigrationDao.get().getPhonesFromReal();

      assertThat(phoneDots).hasSameSizeAs(tempPhones);

      for (int i = 0; i < phoneDots.size(); i++) {
        assertThat(phoneDots.get(i).number).isEqualTo(tempPhones.get(i).number);
        assertThat(phoneDots.get(i).type).isEqualTo(tempPhones.get(i).type);
      }

      List<AddressDot> addressDots = ciaMigrationDao.get().getAddressDots();
      assertThat(addressDots).hasSameSizeAs(tempAddresses);

      for (int i = 0; i < addressDots.size(); i++) {
        assertThat(addressDots.get(i).street).isEqualTo(tempAddresses.get(i).street);
        assertThat(addressDots.get(i).flat).isEqualTo(tempAddresses.get(i).flat);
        assertThat(addressDots.get(i).house).isEqualTo(tempAddresses.get(i).house);
        assertThat(addressDots.get(i).type).isEqualTo(tempAddresses.get(i).type);
      }
    }
  }

  private void insertClients() {
    ciaMigrationDao.get().insertNewClient1(ciaMigrationDao.get().insertNewCharm());
    ciaMigrationDao.get().insertNewClient2(ciaMigrationDao.get().insertNewCharm());
  }

  private void assertAddresses(List<TempAddress> addresses, List<TempAddress> tempAddress) {
    for (int i = 0; i < tempAddress.size(); i++) {
      assertThat(tempAddress.get(i).client_id).isEqualTo(addresses.get(i).client_id);
      assertThat(tempAddress.get(i).flat).isEqualTo(addresses.get(i).flat);
      assertThat(tempAddress.get(i).house).isEqualTo(addresses.get(i).house);
      assertThat(tempAddress.get(i).street).isEqualTo(addresses.get(i).street);
      assertThat(tempAddress.get(i).type).isEqualTo(addresses.get(i).type);
    }
  }

  private void assertPhones(List<TempPhone> phones, List<TempPhone> tempPhones) {
    for (int i = 0; i < phones.size(); i++) {
      assertThat(tempPhones.get(i).client_id).isEqualTo(phones.get(i).client_id);
      assertThat(tempPhones.get(i).number).isEqualTo(phones.get(i).number);
      assertThat(tempPhones.get(i).type).isEqualTo(phones.get(i).type);
    }
  }

  private TempClient createClientWithoutBirth() throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter("build/test_cia.xml", "UTF-8");
    writer.println("<cia>");

    TempClient client1 = new TempClient();
    client1.client_id = "1";
    client1.surname = RND.str(10);
    client1.name = RND.str(10);
    client1.patronymic = RND.str(10);
    client1.gender = RND.str(10);
    client1.birth = null;
    client1.error = "";
    client1.charm = RND.str(10);
    client1.timestamp = new Timestamp(new Date().getTime());

    writer.println(toClientXML(client1));
    writer.println("</cia>");
    writer.close();
    return client1;
  }

  private TempClient createClientWithoutSurname() throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter("build/test_cia.xml", "UTF-8");
    writer.println("<cia>");

    TempClient client1 = new TempClient();
    client1.client_id = "1";
    client1.surname = "";
    client1.name = RND.str(10);
    client1.patronymic = RND.str(10);
    client1.gender = RND.str(10);
    client1.birth = "1980-02-02";
    client1.error = "";
    client1.charm = RND.str(10);
    client1.timestamp = new Timestamp(new Date().getTime());

    writer.println(toClientXML(client1));
    writer.println("</cia>");
    writer.close();
    return client1;
  }

  private TempClient createClientWithoutName() throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter("build/test_cia.xml", "UTF-8");
    writer.println("<cia>");

    TempClient client1 = new TempClient();
    client1.client_id = "1";
    client1.surname = RND.str(10);
    client1.name = "";
    client1.patronymic = RND.str(10);
    client1.gender = RND.str(10);
    client1.birth = "1980-02-02";
    client1.error = "";
    client1.charm = RND.str(10);
    client1.timestamp = new Timestamp(new Date().getTime());

    writer.println(toClientXML(client1));
    writer.println("</cia>");
    writer.close();
    return client1;
  }

  private List<TempClient> createClientXmlFile() throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter("build/test_cia.xml", "UTF-8");
    writer.println("<cia>");
    List<TempClient> clients = new ArrayList<>();

    TempClient client1 = new TempClient();
    client1.client_id = "1";
    client1.surname = RND.str(10);
    client1.name = RND.str(10);
    client1.patronymic = RND.str(10);
    client1.gender = RND.str(10);
    client1.birth = "1980-02-02";
    client1.error = "";
    client1.charm = RND.str(10);
    client1.timestamp = new Timestamp(new Date().getTime());
    writer.println(toClientXML(client1));
    clients.add(client1);

    TempClient client2 = new TempClient();
    client2.client_id = "2";
    client2.surname = RND.str(10);
    client2.name = RND.str(10);
    client2.patronymic = RND.str(10);
    client2.gender = RND.str(10);
    client2.birth = "1912-02-02";
    client2.error = "";
    client2.charm = RND.str(10);
    client2.timestamp = new Timestamp(new Date().getTime());
    writer.println(toClientXML(client2));

    TempClient client3 = new TempClient();
    client3.client_id = "2";
    client3.surname = RND.str(10);
    client3.name = RND.str(10);
    client3.patronymic = RND.str(10);
    client3.gender = RND.str(10);
    client3.birth = "1980-02-02";
    client3.error = "";
    client3.charm = RND.str(10);
    client3.timestamp = new Timestamp(new Date().getTime());
    writer.println(toClientXML(client3));
    clients.add(client3);

    TempClient client4 = new TempClient();
    client4.client_id = "3";
    client4.surname = "";
    client4.name = "";
    client4.patronymic = "";
    client4.gender = "";
    client4.birth = "1980-02-02";
    client4.error = "";
    client4.charm = RND.str(10);
    client4.timestamp = new Timestamp(new Date().getTime());
    writer.println(toClientXML(client4));
    clients.add(client4);

    writer.println("\n</cia>");
    writer.close();
    return clients;
  }


  private List<TempAddress> createAddressXml() throws FileNotFoundException, UnsupportedEncodingException {
    List<TempAddress> addresses = new ArrayList<>();
    PrintWriter writer = new PrintWriter("build/test_cia.xml", "UTF-8");
    writer.println("<cia>");
    TempAddress reg = new TempAddress();
    reg.client_id = "1";
    reg.flat = RND.str(3);
    reg.street = RND.str(3);
    reg.house = RND.str(3);
    reg.type = "REG";

    TempAddress fact = new TempAddress();
    fact.client_id = "1";
    fact.flat = RND.str(3);
    fact.street = RND.str(3);
    fact.house = RND.str(3);
    fact.type = "FACT";
    writer.println(toAddressXML(reg, fact));

    TempAddress reg2 = new TempAddress();
    reg2.client_id = "2";
    reg2.flat = RND.str(3);
    reg2.street = RND.str(3);
    reg2.house = RND.str(3);
    reg2.type = "REG";

    TempAddress fact2 = new TempAddress();
    fact2.client_id = "2";
    fact2.flat = RND.str(3);
    fact2.street = RND.str(3);
    fact2.house = RND.str(3);
    fact2.type = "FACT";
    writer.println(toAddressXML(reg2, fact2));

    TempAddress reg3 = new TempAddress();
    reg3.client_id = "1";
    reg3.flat = RND.str(3);
    reg3.street = RND.str(3);
    reg3.house = RND.str(3);
    reg3.type = "REG";

    TempAddress fact3 = new TempAddress();
    fact3.client_id = "1";
    fact3.flat = RND.str(3);
    fact3.street = RND.str(3);
    fact3.house = RND.str(3);
    fact3.type = "FACT";
    writer.println(toAddressXML(reg3, fact3));

    addresses.add(fact3);
    addresses.add(reg3);

    addresses.add(fact2);
    addresses.add(reg2);

    writer.println("\n</cia>");
    writer.close();
    return addresses;
  }

  private List<TempPhone> createPhones() throws FileNotFoundException, UnsupportedEncodingException {
    List<TempPhone> phones = new ArrayList<>();
    PrintWriter writer = new PrintWriter("build/test_cia.xml", "UTF-8");
    writer.println("<cia>");

    TempPhone mobilePhone = new TempPhone();
    mobilePhone.client_id = "1";
    mobilePhone.number = RND.str(10);
    mobilePhone.type = "MOBILE";

    TempPhone homePhone = new TempPhone();
    homePhone.client_id = "1";
    homePhone.number = RND.str(10);
    homePhone.type = "HOME";
    phones.add(mobilePhone);
    phones.add(homePhone);
    writer.println(toPhoneXML(mobilePhone, homePhone));


    TempPhone mobilePhone1 = new TempPhone();
    mobilePhone1.client_id = "2";
    mobilePhone1.number = RND.str(10);
    mobilePhone1.type = "MOBILE";

    TempPhone homePhone1 = new TempPhone();
    homePhone1.client_id = "2";
    homePhone1.number = RND.str(10);
    homePhone1.type = "HOME";
    phones.add(mobilePhone1);
    phones.add(homePhone1);
    writer.println(toPhoneXML(mobilePhone1, homePhone1));
    writer.println("</cia>");
    writer.close();
    return phones;
  }

  private String toClientXML(TempClient client) {
    return "<client id=\"" + client.client_id + "\">"
      + "\n<surname value=\"" + client.surname + "\"/> "
      + "\n<name value=\"" + client.name + "\"/>"
      + "\n<gender value=\"" + client.gender + "\"/>"
      + "\n<birth value=\"" + client.birth + "\"/>"
      + "\n<charm value=\"" + client.charm + "\"/>"
      + "\n<patronymic value=\"" + client.patronymic + "\"/>"
      + "\n</client>";
  }

  private List<TempPhone> getPhonesFromFullFile() {
    List<TempPhone> phones = new ArrayList<>();

    TempPhone tempPhone = new TempPhone();
    tempPhone.client_id = "1";
    tempPhone.number = "+7-555-555-55-55";
    tempPhone.type = "WORKING";

    phones.add(tempPhone);

    TempPhone tempPhone1 = new TempPhone();
    tempPhone.client_id = "1";
    tempPhone1.number = "+7-666-666-66-66";
    tempPhone1.type = "HOME";
    phones.add(tempPhone1);

    TempPhone tempPhone2 = new TempPhone();
    tempPhone.client_id = "2";
    tempPhone2.number = "+7-777-777-77-77";
    tempPhone2.type = "WORKING";
    phones.add(tempPhone2);

    TempPhone tempPhone3 = new TempPhone();
    tempPhone.client_id = "2";
    tempPhone3.number = "+7-888-888-88-88";
    tempPhone3.type = "HOME";
    phones.add(tempPhone3);

    return phones;
  }


  private List<TempClient> getClientsFromFullFile() {
    List<TempClient> tempClients = new ArrayList<>();
    TempClient tempClient = new TempClient();
    tempClient.client_id = "1";
    tempClient.name = "Client1";
    tempClient.surname = "Client1";
    tempClient.patronymic = null;
    tempClient.gender = "FEMALE";
    tempClient.birth = "1934-04-26";
    tempClients.add(tempClient);

    TempClient tempClient1 = new TempClient();
    tempClient1.client_id = "2";
    tempClient1.name = "Client3";
    tempClient1.surname = "Client3";
    tempClient1.patronymic = null;
    tempClient1.gender = "FEMALE";
    tempClient1.birth = "1934-04-26";
    tempClients.add(tempClient1);

    return tempClients;
  }

  private List<TempAddress> getAddressesFromFullFile() {
    List<TempAddress> addresses = new ArrayList<>();
    TempAddress address = new TempAddress();
    address.client_id = "1";
    address.street = "street1";
    address.house = "house1";
    address.flat = "flat1";
    address.type = "FACT";
    addresses.add(address);

    TempAddress address1 = new TempAddress();
    address1.client_id = "1";
    address1.street = "street2";
    address1.house = "house2";
    address1.flat = "flat2";
    address1.type = "REG";
    addresses.add(address1);

    TempAddress address2 = new TempAddress();
    address2.client_id = "2";
    address2.street = "street33";
    address2.house = "house33";
    address2.flat = "flat33";
    address2.type = "FACT";
    addresses.add(address2);

    TempAddress address3 = new TempAddress();
    address3.client_id = "2";
    address3.street = "street44";
    address3.house = "house44";
    address3.flat = "flat44";
    address3.type = "REG";
    addresses.add(address3);

    return addresses;
  }

  private void createFullFile() throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter("build/test_cia.xml", "UTF-8");
    writer.println("<cia>");
    writer.println("<client id=\"1\"> <!-- 1 -->\n" +
      "    <address>\n" +
      "      <fact street=\"street1\" house=\"house1\" flat=\"flat1\"/>\n" +
      "      <register street=\"street2\" house=\"house2\" flat=\"flat2\"/>\n" +
      "    </address>\n" +
      "    <charm value=\"Charm1\"/>\n" +
      "    <workPhone>+7-555-555-55-55</workPhone>\n" +
      "    <homePhone>+7-666-666-66-66</homePhone>\n" +
      "    <gender value=\"FEMALE\"/>\n" +
      "    <surname value=\"Client1\"/>\n" +
      "    <birth value=\"1934-04-26\"/>\n" +
      "    <name value=\"Client1\"/>\n" +
      "  </client>");
    writer.println("<client id=\"2\">\n" +
      "    <address>\n" +
      "      <fact street=\"street3\" house=\"house3\" flat=\"flat3\"/>\n" +
      "      <register street=\"street4\" house=\"house4\" flat=\"flat4\"/>\n" +
      "    </address>\n" +
      "    <charm value=\"Charm2\"/>\n" +
      "    <workPhone>+7-777-777-77-77</workPhone>\n" +
      "    <homePhone>+7-888-888-88-88</homePhone>\n" +
      "    <gender value=\"FEMALE\"/>\n" +
      "    <surname value=\"Client2\"/>\n" +
      "    <birth value=\"1934-04-26\"/>\n" +
      "    <name value=\"Client2\"/>\n" +
      "  </client>");
    writer.println("<client id=\"2\">\n" +
      "    <address>\n" +
      "      <fact street=\"street33\" house=\"house33\" flat=\"flat33\"/>\n" +
      "      <register street=\"street44\" house=\"house44\" flat=\"flat44\"/>\n" +
      "    </address>\n" +
      "    <charm value=\"Charm2\"/>\n" +
      "    <gender value=\"FEMALE\"/>\n" +
      "    <surname value=\"Client3\"/>\n" +
      "    <birth value=\"1934-04-26\"/>\n" +
      "    <name value=\"Client3\"/>\n" +
      "  </client>");
    writer.println("<client id=\"6-IHP-4P-IT-3nQMG0tQqB\"> <!-- 2 -->\n" +
      "    <address>\n" +
      "      <fact street=\"Гяd5птzикИ9JЕXЕКCdзХ\" house=\"0ф\" flat=\"Nm\"/>\n" +
      "      <register street=\"ДЭMбmРЗzoБиQПMтHрксФ\" house=\"щП\" flat=\"Cт\"/>\n" +
      "    </address>\n" +
      "    <birth value=\"1985-06-04\"/>\n" +
      "    <name value=\"дrS4чЪIhыт\"/>\n" +
      "    <charm value=\"АцZДЁИдхЙЩ\"/>\n" +
      "    <gender value=\"MALE\"/>\n" +
      "  </client>");
    writer.println("</cia>");
    writer.close();
  }

  private String toAddressXML(TempAddress reg, TempAddress fact) {
    return
      "<client id=\"" + reg.client_id + "\">"
        + "\n<address>"
        + "\n\t<fact street=\"" + fact.street + "\" house=\"" + fact.house + "\" flat=\"" + fact.flat + "\"/>"
        + "\n\t<register street=\"" + reg.street + "\" house=\"" + reg.house + "\" flat=\"" + reg.flat + "\"/>"
        + "\n</address>"
        + "\n</client>";
  }

  private String toPhoneXML(TempPhone mobile, TempPhone home) {
    return "<client id=\"" + mobile.client_id + "\">"
      + "\n<mobilePhone>" + mobile.number + "</mobilePhone>"
      + "\n<homePhone>" + home.number + "</homePhone>"
      + "\n</client>";
  }

  private void assertClients(List<TempClient> clients1, List<TempClient> clients2) {
    for (int i = 0; i < clients1.size(); i++) {
      assertThat(clients1.get(i).client_id).isEqualTo(clients2.get(i).client_id);
      assertThat(clients1.get(i).name).isEqualTo(clients2.get(i).name);
      assertThat(clients1.get(i).surname).isEqualTo(clients2.get(i).surname);
      assertThat(clients1.get(i).patronymic).isEqualTo(clients2.get(i).patronymic);
    }
  }

  private TempClient createClient() {
    TempClient client = new TempClient();
    client.client_id = RND.str(10);
    client.name = RND.str(10);
    client.surname = RND.str(10);
    client.patronymic = RND.str(10);
    client.gender = RND.str(4);
    client.birth = "1980-09-09";
    client.charm = RND.str(10);
    client.error = "";
    client.timestamp = new Timestamp(new Date().getTime());
    return client;
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
