package kz.greetgo.sandbox.db.register_impl;

import kz.greetgo.depinject.core.BeanGetter;
import kz.greetgo.sandbox.controller.enums.AddressType;
import kz.greetgo.sandbox.controller.enums.GenderType;
import kz.greetgo.sandbox.controller.enums.PhoneNumberType;
import kz.greetgo.sandbox.controller.model.*;
import kz.greetgo.sandbox.controller.register.ClientRegister;
import kz.greetgo.sandbox.db.stand.model.ClientAddressDot;
import kz.greetgo.sandbox.db.stand.model.ClientDot;
import kz.greetgo.sandbox.db.stand.model.ClientPhoneNumberDot;
import kz.greetgo.sandbox.db.test.dao.ClientTestDao;
import kz.greetgo.sandbox.db.test.util.ParentTestNg;
import kz.greetgo.util.RND;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

@SuppressWarnings({"deprecation", "WeakerAccess", "ConstantConditions"})
public class ClientRegisterImplTest extends ParentTestNg {

  public BeanGetter<ClientRegister> clientRegister;
  public BeanGetter<ClientTestDao> clientTestDao;
  public BeanGetter<IdGenerator> idGenerator;


  @Test
  void getClientsSizeTest() {
    this.clientTestDao.get().clear();

    List<ClientDot> clients = new ArrayList<>();
    for (int i = 0; i < RND.plusInt(1000); i++) {
      ClientDot cd = this.rndClientDot();
      this.clientTestDao.get().insertClientDot(cd);
      clients.add(cd);
    }

    //test1
    {
      String filter = null;
      long result = this.clientRegister.get().getClientsSize(filter);
      assertThat(result).isEqualTo(clients.size());
    }
    //test2
    {
      String[] filters = {
        clients.get(RND.plusInt(clients.size())).name,
        clients.get(RND.plusInt(clients.size())).surname,
        clients.get(RND.plusInt(clients.size())).patronymic,
        clients.get(RND.plusInt(clients.size())).getFIO()};

      for (String filter : filters) {

        //
        //
        long result = this.clientRegister.get().getClientsSize(filter);
        //
        //

        long expected = clients.stream().filter(o -> o.getFIO().toLowerCase().contains(filter.toLowerCase())).count();
        assertThat(result).isEqualTo(expected);
      }


    }


  }

  @Test
  void removeClientsTest() {
    this.clientTestDao.get().clear();
    List<String> ids = new ArrayList<>();

    for (int i = 0; i < RND.plusInt(1000); i++) {
      ClientDot cd = this.rndClientDot();
      this.clientTestDao.get().insertClientDot(cd);
      ids.add(cd.id);
    }

    //
    //
    int deleted = this.clientRegister.get().remove(ids);
    //
    //

    assertThat(deleted).isEqualTo(ids.size());
    List<ClientDot> list = new ArrayList<>();
    assertThat(list).isEmpty();

//    for (String id : ids) {
//      assertThat(list.stream().anyMatch(o -> o.id.equals(id))).isFalse();
//    }
  }

  @Test
  public void updateClientTest() {
    this.clientTestDao.get().clear();
    ClientDot cd = this.rndClientDot();
    this.clientTestDao.get().insertClientDot(cd);
    ClientAddressDot actualAddress = rndAddress(cd.id, AddressType.FACT);
    ClientAddressDot registerAddress = rndAddress(cd.id, AddressType.REG);
    this.clientTestDao.get().insertAddress(actualAddress);
    this.clientTestDao.get().insertAddress(registerAddress);

    ClientToSave test1 = rndClientToSave(cd.id); // +ClientDetail +2addresses +3numbers
    //test1
    {
      ClientPhoneNumberDot number1 = rndPhoneNumber(cd.id, PhoneNumberType.WORK);
      this.clientTestDao.get().insertPhone(number1); // number directly insterted to db
      test1.numbersToDelete.add(number1.toClientPhoneNumber());  // -1 number

      //
      //
      this.clientRegister.get().update(test1);
      //
      //

      ClientDetail clientDetail = this.clientTestDao.get().detail(test1.id);
      this.assertClientDetail(clientDetail, new ClientDot(test1));

      ClientAddress regAddress = this.clientTestDao.get().getAddres(test1.id, AddressType.REG);
      ClientAddress actAddress = this.clientTestDao.get().getAddres(test1.id, AddressType.FACT);
      this.assertClientAddres(actAddress, new ClientAddressDot(test1.id, test1.actualAddress));
      this.assertClientAddres(regAddress, new ClientAddressDot(test1.id, test1.registerAddress));

      List<ClientPhoneNumber> numberList = this.clientTestDao.get().getNumbersById(test1.id);
      assertThat(numberList.isEmpty()).isFalse();
      assertThat(numberList.size()).isEqualTo(test1.numersToSave.size());
      assertThat(numberList.stream().anyMatch(o -> o.number.equals(number1.number))).isFalse();
    }

    //test2
    {

      ClientToSave test2 = rndClientToSave(cd.id);

      List<ClientPhoneNumber> numberList = this.clientTestDao.get().getNumbersById(test2.id);
      assertThat(numberList.isEmpty()).isFalse();

      ClientPhoneNumberToSave toEited = new ClientPhoneNumberDot(test2.id, numberList.get(0)).toClientPhoneNumberToSave();
      toEited.oldNumber = toEited.number;
      toEited.number = RND.str(10);
      test2.numersToSave.add(toEited);

      //
      //
      this.clientRegister.get().update(test2);
      //
      //

      numberList = this.clientTestDao.get().getNumbersById(test2.id);
      assertThat(numberList.stream().anyMatch(o -> o.number.equals(toEited.number))).isTrue();

    }


  }

  @Test
  public void addClientTest() {
    this.clientTestDao.get().clear();
    ClientToSave client = rndClientToSave(null);

    //
    //
    this.clientRegister.get().add(client);
    //
    //

    ClientDetail clientDetail = this.clientTestDao.get().detail(client.id);
    this.assertClientDetail(clientDetail, new ClientDot(client));
    ClientAddress regAddress = this.clientTestDao.get().getAddres(client.id, AddressType.REG);
    ClientAddress actAddress = this.clientTestDao.get().getAddres(client.id, AddressType.FACT);
    this.assertClientAddres(actAddress, new ClientAddressDot(client.id, client.actualAddress));
    this.assertClientAddres(regAddress, new ClientAddressDot(client.id, client.registerAddress));

    List<ClientPhoneNumber> numberList = this.clientTestDao.get().getNumbersById(client.id);
    assertThat(numberList.isEmpty()).isFalse();
    assertThat(numberList.size()).isEqualTo(client.numersToSave.size());

    for (ClientPhoneNumberToSave cpn : client.numersToSave) {
      assertThat(numberList.stream().anyMatch(o -> o.number.equals(cpn.number) && o.type.equals(cpn.type) && o.client.equals(cpn.client))).isTrue();
    }
  }

  @Test
  public void getDetailTest() throws Exception {

    this.clientTestDao.get().clear();
    ClientDot c = this.rndClientDot();
    this.clientTestDao.get().insertClientDot(c);
    ClientPhoneNumberDot number1 = rndPhoneNumber(c.id, PhoneNumberType.WORK);
    this.clientTestDao.get().insertPhone(number1);
    ClientAddressDot actualAddress = rndAddress(c.id, AddressType.FACT);
    ClientAddressDot registerAddress = rndAddress(c.id, AddressType.REG);
    this.clientTestDao.get().insertAddress(actualAddress);
    this.clientTestDao.get().insertAddress(registerAddress);


    //
    //
    ClientDetail detail = this.clientRegister.get().detail(c.id);
    //
    //

    this.assertClientDetail(detail, c);
    assertThat(detail.phoneNumbers).isNotNull();
    assertThat(detail.actualAddress).isNotNull();
    assertThat(detail.registerAddress).isNotNull();
    assertThat(detail.phoneNumbers.size()).isEqualTo(1);
    this.assertClientAddres(detail.actualAddress, actualAddress);
    this.assertClientAddres(detail.registerAddress, registerAddress);
    this.assertPhoneNumber(detail.phoneNumbers.get(0), number1);

  }

  @SuppressWarnings("SameParameterValue")
  private ClientToSave rndClientToSave(String id) {
    ClientToSave client = new ClientToSave();
    client.id = id;
    client.name = RND.str(10);
    client.surname = RND.str(10);
    client.patronymic = RND.str(10);
    client.gender = GenderType.MALE;
    client.birthDate = RND.dateYears(1996, 2018);
    client.charm = RND.str(10);

    client.actualAddress = rndAddress(id, AddressType.FACT).toClientAddress();
    client.registerAddress = rndAddress(id, AddressType.REG).toClientAddress();

    client.numbersToDelete = new ArrayList<>();
    client.numersToSave = new ArrayList<>();

    client.numersToSave.add(rndPhoneNumber(id, PhoneNumberType.WORK).toClientPhoneNumberToSave());
    client.numersToSave.add(rndPhoneNumber(id, PhoneNumberType.MOBILE).toClientPhoneNumberToSave());
    client.numersToSave.add(rndPhoneNumber(id, PhoneNumberType.HOME).toClientPhoneNumberToSave());

    return client;
  }

  private void assertPhoneNumber(ClientPhoneNumber target, ClientPhoneNumberDot assertion) {
    if (target == null && assertion == null)
      return;
    assertThat(target).isNotNull();
    assertThat(target.client).isEqualTo(assertion.client);
    assertThat(target.number).isEqualTo(assertion.number);
    assertThat(target.type).isEqualTo(assertion.type);
  }

  private void assertClientAddres(ClientAddress target, ClientAddressDot assertion) {
    if (target == null && assertion == null)
      return;
    assertThat(target).isNotNull();
    assertThat(target.client).isEqualTo(assertion.client);
    assertThat(target.street).isEqualTo(assertion.street);
    assertThat(target.house).isEqualTo(assertion.house);
    assertThat(target.flat).isEqualTo(assertion.flat);
    assertThat(target.type).isEqualTo(assertion.type);
  }

  private ClientAddressDot rndAddress(String id, AddressType type) {
    ClientAddressDot address = new ClientAddressDot();
    address.client = id;
    address.type = type;

    address.street = RND.str(10);
    address.house = RND.str(10);
    return address;
  }

  private ClientPhoneNumberDot rndPhoneNumber(String clientId, PhoneNumberType type) {
    ClientPhoneNumberDot number = new ClientPhoneNumberDot();
    number.client = clientId;
    number.type = type;
    number.number = idGenerator.get().newId();

    return number;
  }

  private void assertClientDetail(ClientDetail target, ClientDot assertion) {
    if (target == null && assertion == null)
      return;
    assertThat(target).isNotNull();
    assertThat(target).isNotNull();
    assertThat(target.name).isEqualTo(assertion.name);
    assertThat(target.surname).isEqualTo(assertion.surname);
    assertThat(target.patronymic).isEqualTo(assertion.patronymic);
    assertThat(target.gender).isEqualTo(assertion.gender);

    assertThat(target.birthDate.getDay()).isEqualTo(assertion.birthDate.getDay());
    assertThat(target.birthDate.getMonth()).isEqualTo(assertion.birthDate.getMonth());
    assertThat(target.birthDate.getYear()).isEqualTo(assertion.birthDate.getYear());

    assertThat(target.charm).isEqualTo(assertion.charm);
    assertThat(target.id).isEqualTo(assertion.id);
  }

  private ClientDot rndClientDot() {
    ClientDot c = new ClientDot();
    c.id = idGenerator.get().newId();
    c.name = RND.str(10);
    c.surname = RND.str(10);
    c.patronymic = RND.str(10);
    c.charm = RND.str(10);
    c.gender = GenderType.MALE;
    c.birthDate = new Date();

    return c;
  }


}
