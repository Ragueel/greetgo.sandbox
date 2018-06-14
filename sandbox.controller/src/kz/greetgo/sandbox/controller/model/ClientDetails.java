package kz.greetgo.sandbox.controller.model;

import java.util.Arrays;
import java.util.Date;

public class ClientDetails {

  public int id;
  public String name;
  public String surname;
  public String patronymic;
  public String gender;
  public Date birthDate;


  public int age = 0;

  public int charm;

  // FIXME: 14.06.18 Сделать List вместо массива
  public Address[] addresses;

  public Phone[] phones;

  @Override
  public String toString() {
    return "Client{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", surname='" + surname + '\'' +
      ", patronymic='" + patronymic + '\'' +
      ", gender='" + gender + '\'' +
      ", birthDate='" + birthDate + '\'' +
      ", age=" + age +
      ", charm=" + charm +
      ", addresses=" + Arrays.toString(addresses) +
      ", phones=" + Arrays.toString(phones) +
      '}';
  }
}
